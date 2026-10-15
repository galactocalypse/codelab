# Codelab message evolution & business-version contract

**Status:** implemented (§1–§6), then restructured per approval — see §7 for the final shape:
CDC-emitted events, a separate version-less job bus, and message size caps.

This design adds a **business version** to every Codelab pub/sub message: publishers are
forced to declare the version of the event they are emitting, consumers declare which
versions they can handle, and the framework routes *any* message whose version a consumer
cannot handle **directly to the DLQ** — bypassing nack/retry entirely — **without ever
dropping a message**.

The business version is intentionally **independent of Pulsar's internal `schemaVersion`**
(the schema-metadata version Pulsar maintains when a topic's schema evolves). Business
version is a *contract* level version carried as a message property; `schemaVersion` is a
*schema* level artifact managed by Pulsar. Neither replaces the other.

---

## 1. Current messaging model (baseline)

Identities in this repo are the `spring.eventbus` types in `codelab-commons` plus the
framework wiring in `codelab-megalith`/`com.codelab.core.eventbus`.

**Publish side**

- `@CodelabTopic(name)` on an interface extending `CodelabEventPublisher<T>`:
  `codelab-commons/.../eventbus/CodelabTopic.java`, `CodelabEventPublisher.java`.
- `CodelabPulsarRegistryUtils.registerPulsarPublishers(...)` scans each module for those
  interfaces, resolves the physical topic
  (`persistent://codelab-<module>/<ns>/<kind>.<logical>` — the kind prefix is §7.4),
  and registers a `CodelabPublisherFactoryBean`.
- `CodelabPublisherFactoryBean` builds a `Schema.JSON(payloadType)` producer and a JDK
  proxy; `PublisherInvocationHandler` (`codelab-megalith/.../eventbus/PublisherInvocationHandler.java`)
  turns `publish(...)` into `producer.newMessage().value(event)[.key(k)].send()`.
- **No custom properties are ever set today** — messages carry only key + payload.

**Consume side**

- `@CodelabSubscription` (`codelab-commons/.../eventbus/CodelabSubscription.java`) on a
  class implementing `CodelabEventConsumer<T>` (`void consume(T event)`).
- `CodelabPulsarRegistryUtils.registerPulsarSubscriptions(...)` registers the consumer bean;
  `CodelabPulsarListenerConfigurer.configurePulsarListeners(...)` builds a
  `MethodPulsarListenerEndpoint` per consumer and registers it with Spring Pulsar's
  default container factory (`null` in `registrar.registerEndpoint(endpoint, null)`).
- Endpoint config today: `AckMode.RECORD`, `SchemaType.NONE`, `deadLetterPolicyRef`
  looked up by bean name, consumer builder customizer sets `initialPosition`/`ackTimeout`.
- **Failure handling today:** the listener `consume(T)` *returns* ⇒ ack (record); it
  *throws* ⇒ the container negative-acks (`DefaultPulsarMessageListenerContainer.Listener`,
  `dispatchMessageToListener`). With `DeadLetterPolicy.maxRedeliverCount=N` (e.g. movies'
  `movieDeadLetterPolicy`, `maxRedeliverCount(3)`), Pulsar's client forwards the message to
  the DLQ once redelivery count exceeds N. That is the **after-retries** route: every
  deterministically-dead message is nacked and redelivered (with ack-timeout / redelivery
  delays) *before* finally reaching the DLQ.

---

## 2. Goals

1. **Mandate** a business version at publish time (compile-time, every call site).
2. Consumers declare the set of versions they can parse (`getSupportedBusinessVersions()`).
3. The framework checks the version **before** invoking the consumer (and before any
   payload de-serialization), and routes unsupported messages **directly to the DLQ** —
   no nack, no retry, no downtime spent re-poisoning.
4. Routing must **never lose a message**.
5. Business version and Pulsar `schemaVersion` stay fully independent.

---

## 3. Design

### 3.1 The version value type — `BusinessVersion` (commons)

A small immutable value type in `codelab-commons`/`com.codelab.common.spring.eventbus`:

- backed by a validated `String` (non-blank, trimmed, `[A-Za-z0-9][A-Za-z0-9._-]{0,63}`);
- `equals`/`hashCode`/`toString` on the raw value;
- static constants:
  - `BusinessVersion.LEGACY = of("legacy")` — the sentinel used for messages **without**
    the property (see §3.5);
- framework message-property constant lives next to it:
  `CodelabMessageProperties.BUSINESS_VERSION = "codelab-business-version"`.

Version semantics are **exact, opaque string match** (set membership). No range/major
matching in this iteration — see §6 for evolution.

### 3.2 Publish contract — version is a mandatory parameter

`codelab-commons/.../eventbus/CodelabEventPublisher.java` becomes:

```java
public interface CodelabEventPublisher<T> {
  void publish(BusinessVersion version, T event);
  void publish(String key, BusinessVersion version, T event);
}
```

- The old versionless `publish(T)` / `publish(String key, T)` overloads are **removed**, so
  the version is *mandated at compile time* at every call site (3 call sites in this repo).
- The new overloads cannot collide with the old `publish(String key, T)` — the second arg
  is now `BusinessVersion`, a distinct type, and the no-key form leads with `BusinessVersion`.

`PublisherInvocationHandler.invoke(...)` is updated to write the property onto the message
during preparation:

```java
producer.newMessage().property(CodelabMessageProperties.BUSINESS_VERSION, version.value())
        .key(key).value(event).send();
```

(plus the existing `validatePayload` guard; the handler re-validates non-blank defensively).

### 3.3 Consume contract — consumer declares supported versions

`codelab-commons/.../eventbus/CodelabEventConsumer.java` becomes:

```java
public interface CodelabEventConsumer<T> {
  Set<BusinessVersion> getSupportedBusinessVersions();
  void consume(T event);
}
```

- Abstract (non-default), so every consumer is forced to answer — and the framework can
  **fail startup** when a consumer returns an empty set.

### 3.4 Framework dispatcher — gate before deserialization, route before processing

A new `CodelabVersionAwareMessageDispatcher<T>` (megalith, `com.codelab.core.eventbus`) is
the actual Spring Pulsar listener for each consumer endpoint:

```java
public void dispatch(org.apache.pulsar.client.api.Message<byte[]> message)
```

The configurer points each endpoint at the dispatcher (`endpoint.setBean(dispatcher)`,
`endpoint.setMethod(dispatchMethod)`) instead of at `consumer.consume`. It still
`findConsumeMethod(...)` to validate the consumer, and keeps `AckMode.RECORD` +
`SchemaType.NONE` (the `Message<byte[]>` parameter makes the container resolve `Schema.BYTES`
— verified against `DefaultSchemaResolver`, which maps `byte[]` → `Schema.BYTES`).

Dispatch logic (single pass, one message):

1. **Read version from properties only** (no payload access, no de-serialization):
   `raw = message.getProperty(BUSINESS_VERSION)`;
   `effective = (raw blank) ? LEGACY : BusinessVersion.of(raw)`.
2. `supported = consumer.getSupportedBusinessVersions()` (read once at startup).
3. **Supported** ⇒ decode the payload exactly as the publisher encoded it —
   `Schema.JSON(payloadType).decode(message.getData())` (same `Schema.JSON` the producer
   uses; `Schema.decode(byte[])` verified present in Pulsar 4.2.4) — then
   `delegate.consume(payload)` and return. A normal return is acked by the container, as
   today. Any thrown exception keeps today's nack → retry → native `DeadLetterPolicy` route.
4. **Unsupported** ⇒ forward to the DLQ (§3.6) **before** invoking the consumer. On
   success, return (container acks the source message — the equivalent of "acked, gone,
   preserved in DLQ"). On DLQ failure, throw (container negative-acks ⇒ redelivery later ⇒
   the routing is retried — the message is never silently dropped).

Because the check happens *before* decode, an unsupported version whose new shape would
fail old-schema de-serialization (field type changes, incompatible enums, etc.) still goes
straight to the DLQ instead of being nacked/retried or 50/50-poisoning.

### 3.5 Missing property / legacy messages

Messages produced *before* this feature (or by non-Codelab producers) carry no property.
Policy:

- missing/blank ⇒ treated as `BusinessVersion.LEGACY`;
- if the consumer declares `LEGACY` in its supported set ⇒ processed normally;
- otherwise ⇒ routed to DLQ with reason `MISSING_BUSINESS_VERSION`.

This keeps the contract strict while giving a deterministic, per-consumer escape hatch for
rollouts (see §4 — movies' `Earliest` backlog matters here).

### 3.6 DLQ routing — forward-then-ack, never lose

New `CodelabDlqRouter` bean (megalith, `com.codelab.core.eventbus`, registered in
`MegalithConfiguration`):

- lazily creates and caches a `Schema.BYTES` producer per DLQ topic
  (`pulsarClient.newProducer(Schema.BYTES).topic(dlq).enableBatching(false)`), closed on
  context close (`DisposableBean`);
- `publishToDlq(...)` copies the message **synchronously**:
  - payload = original raw bytes (`message.getData()`, byte-for-byte — schema-agnostic);
  - key (`getKey()`) and event time (`getEventTime()`) preserved;
  - **all** original properties preserved (so Pulsar's own `ORIGIN_MESSAGE_ID` /
    `REAL_TOPIC` convention and any other headers survive);
  - `codelab.dlq.*` metadata added: `reason`, `observed.business.version`,
    `consumer`, `original.topic`, `original.subscription`, `original.messageId`, `routedAt`;
  - `send()` is **synchronous**: it returns only after the broker has accepted the DLQ copy.

**DLQ topic name.** Same rule the Pulsar consumer applies (`ConsumerImpl` +
`RetryMessageUtil.getDLQTopic`, verified in `pulsar-client` 4.2.4):

- if the subscription has a `deadLetterPolicyRef` whose policy sets an explicit
  `deadLetterTopic` ⇒ use it;
- else ⇒ `{resolvedTopic}-{qualifiedSubscription}-DLQ`
  (e.g. `persistent://codelab-movies/local/job.pending-movies-movies.movies-processor-DLQ` —
  matches what the native policy already writes today).

This means version-unsupported entries and nack-exhausted processing-failure entries land
in the **same DLQ topic**, with the same `-DLQ` convention ops already knows.

### 3.7 No-loss analysis (the ordering that guarantees it)

Forward-then-ack on the *source* topic is what makes it loss-free:

```
unsupported message
   │
   ├─ 1. publish raw copy to DLQ  (synchronous, blocks until broker-accepted)
   │        │  success ───────────────────────────────┐
   │        └  failure ⇒ throw ⇒ container nacks ⇒    │
   │                     redelivery later (retry)     │
   ▼                                                  ▼
2. return ⇒ container acks source message      message stays in source topic
```

- **DLQ publish fails** (broker down, DLQ topic unwritable): we throw; the container
  negative-acks; the message is redelivered and the routing retried. It is never dropped.
  If a `DeadLetterPolicy` is configured, exhaustion of *those* nacks makes Pulsar's native
  forwarder write the same message to the same DLQ topic — same destination, still preserved.
- **DLQ publish succeeds, ack fails / process crashes between the two:** the source message
  is redelivered and re-routed ⇒ a **duplicate** DLQ entry. This is the standard
  at-least-once DLQ guarantee (Pulsar's own forwarder has precisely the same window:
  `sendAsync` → `acknowledgeAsync`, verified in `ConsumerImpl.processPossibleToDLQ`).
- **DLQ topic doesn't exist:** auto-created by the broker under default settings; if topic
  creation/permissions are denied, the failure path above applies (message stays visible and
  safe in the source topic backlog).

Net guarantee: **exactly-once delivery to the consumer's handle, at-least-once delivery to
the DLQ** — identical to the platform's existing DLQ semantics, minus the retries.

### 3.8 What changes and what doesn't

- **Unchanged:** `@CodelabTopic`, `@CodelabSubscription`, the registry setup, per-module
  tenant/namespace wiring, `deadLetterPolicyRef` semantics for *processing* failures,
  `AckMode.RECORD`, ack timeout, Pulsar `schemaVersion` behavior.
- **Changed (commons):** `CodelabEventPublisher` + `CodelabEventConsumer` contracts; new
  `BusinessVersion`, `CodelabMessageProperties`.
- **Changed (megalith):** `PublisherInvocationHandler` (stamp property);
  `CodelabPulsarListenerConfigurer` (dispatcher wiring + startup validation); new
  `CodelabVersionAwareMessageDispatcher`, `CodelabDlqRouter`; `MegalithConfiguration` (bean).
- **Module call sites:** `OrderServiceImpl` (2 publishes), `MovieFeedRunner` (1 publish),
  `MovieProcessor`, `OrderCreatedEventProcessor`, `OrderUpdatedEventProcessor` (+ their
  tests) adopt the new contracts.

---

## 4. Rollout / compatibility

1. **Deploy publishers first** (same release): every new publish carries the property.
2. **Consumers** initial `getSupportedBusinessVersions()` = `{ v1, LEGACY }` — because
   movies' subscription reads `Earliest`, any unacked/backlogged pre-feature messages are
   `legacy` and must not be DLQ'd during the cutover.
3. After the backlog drains, tighten consumers to `{ v1 }` (or whatever) — the LEGACY entry
   can then be dropped. Legitimately-old messages are *already* in the DLQ at that point,
   replayable with existing tooling.

## 5. Verification plan

- **Unit:** publisher handler stamps `business-version` on both overloads; rejects blank.
- **Unit:** dispatcher — supported ⇒ delegate invoked with decoded payload; unsupported ⇒
  delegate not invoked, router called; property missing ⇒ LEGACY rule; router failure ⇒
  exception propagates (negative-ack path).
- **Unit:** router — payload bytes identical, all original properties preserved, `key` /
  `eventTime` preserved, `codelab.dlq.*` metadata present.
- **Integration** (docker-compose pulsar profile): publish `v1`+`v2` on `job.pending-movies`,
  consumer supports only `v1` ⇒ `v2` lands in the DLQ with **zero** redeliveries while `v1`
  processes normally; DLQ entry carries full metadata. Also verifies the `BYTES` consumer
  attaches cleanly to the JSON-schema topic (default broker `schemaValidationEnforced=false`;
  `Schema.BYTES` is universally allowed — recommended to prove in-test).
- **Broker-level E2E** (Testcontainers, automated): `CdcJobEventEndToEndTest` boots the messaging
  stack over a real standalone Pulsar container and asserts that a seeded Debezium insert envelope
  on `job.orders-cdc.public.orders` is normalized into a version-stamped `OrderCreatedEvent` and
  observed on `event.created-orders`. This exercises the kind-prefixed names at both ends, the real
  JSON producer/consumer schemas, the registered subscription topology, and the version gate on live
  traffic. Debezium is out of the loop (envelope seeded directly) so the test stays fast and
  deterministic; it is skipped when Docker is unavailable.
- **Broker-level job E2E** (Testcontainers, automated): `JobBusEndToEndTest` covers the
  application-driven job path — a manual publish through the real `CodelabJobPublisher` proxy
  (size guard, keying, no version property) consumed by a real `CodelabJobConsumer` listener.
  Jobs are never produced by CDC, so this is the only way the job-publish handler is exercised
  against a broker.
- `./gradlew spotlessApply check` per the repo runbook (movies + megalith).

## 6. Explicitly out of scope (future candidates)

- Range / pattern version matching (e.g. "supports v1.x"); matching is exact-set today.
- DLQ for *processing* failures is still the native nack-then-`DeadLetterPolicy` route; only
  version-routing skips retries. A "poison directly to DLQ" policy for other deterministic
  failures could reuse `CodelabDlqRouter` later.
- Partitioned topics: the `-DLQ` naming above is the non-partitioned convention; Codelab
  topics are created non-partitioned today. A partitioned deployment needs per-partition DLQ
  naming (same caveat exists for Pulsar's native formula).
- Per-partition or per-key ordering guarantees across versions (mixed-version streams are the
  business's responsibility).
- Runtime-refreshable version sets (the consumer's supported set is read once at startup).

---

## 7. Approved restructure: CDC-emitted events + a separate job bus

The implementation went through a second design round in which four forks were decided:
events are emitted by **full CDC**, heavy work moves to a **job bus** (with renamed
annotations), messages are size-capped **1KB (events) / 100KB (jobs)** at the app with a
**1MB** broker-wide ceiling, and every physical topic carries a **kind prefix** (`event.*`
/ `job.*`, §7.4). The framework itself (§3 version gate, DLQ router, versioned
contracts) is unchanged; the call sites and the bus split changed.

### 7.1 Business events are emitted by CDC, not by services

- `OrderServiceImpl` publishes nothing (this also removed a real ordering bug: `updateStatus`
  used to publish *before* the flush).
- Infra: Debezium Server (`quay.io/debezium/server:3.6.3.Final`, compose profile `debezium`)
  captures `public.orders` via Postgres logical replication (`wal_level=logical` on the
  postgres service command) and streams raw row envelopes to
  `persistent://codelab-orders/local/job.orders-cdc.public.orders` — configured in
  `docker/debezium/application.properties` (its `topic.prefix=job.orders-cdc` carries the
  job kind prefix so the produce side matches what the framework resolves on the consume
  side; sink tenant/namespace mirror the module tenant; plain JSON envelope,
  `tombstones.on.delete=false`, offsets on the `debezium_data` volume).
- `OrderCdcNormalizer` (orders module) consumes that topic **as a job**
  (`@CodelabJobSubscription`, subscription `orders-cdc-normalizer`, `Earliest`,
  `deadLetterPolicyRef=orderCdcDeadLetterPolicy`) and maps row ops to events, stamping
  `BusinessVersion.of("v1")` at the single point a domain event is born: `c` →
  `OrderCreatedEvent` (keyed by orderId), `u` → `OrderUpdatedEvent` (unkeyed); `d`/`r`
  ignored — the event contract covers create/update only. Events carry only `orderId`, so
  no joins are needed from the row image (`CdcOrderChange`/`CdcOrderRow` model just `id`
  plus `op`/`before`/`after` and ignore the rest of the envelope).

### 7.2 Jobs are a separate contract: no business version, native retry

Heavy async work is not a versioned fact. New commons package
`com.codelab.common.spring.jobbus`:

| | Events | Jobs |
|---|---|---|
| Publisher | `CodelabEventPublisher` — `publish(version, T)` | `CodelabJobPublisher` — `publish(T)` / `publish(key, T)` |
| Consumer | `CodelabEventConsumer` — `consume` + `getSupportedBusinessVersions()` | `CodelabJobConsumer` — `consume` only |
| Annotations | `@CodelabTopic`, `@CodelabSubscription` | `@CodelabJobTopic`, `@CodelabJobSubscription` |
| Dispatcher | `CodelabVersionAwareMessageDispatcher` (gate → `CodelabDlqRouter`) | `CodelabJobMessageDispatcher` (decode → delegate, failures propagate) |
| On failure | unsupported version → DLQ without retry; processing failure → nack → `DeadLetterPolicy` | nack → `DeadLetterPolicy` (no gate at all) |
| Defaults | `Key_Shared`, `Latest`, concurrency 1, `ackTimeoutSeconds=60` | same annotation defaults |

- Job consumers: `MovieProcessor` (`pending-movies` / `movies-processor`) and
  `OrderCdcNormalizer`. Job publisher: `MovieJobPublisher` — payload `MovieJob`, renamed
  from `MovieEvent` along with the runner/processor.
- `OrderCreatedEventPublisher`/`OrderUpdatedEventPublisher` stay on the **event** bus: they
  are the normalizer's bridge onto the versioned side.
- Registration: `CodelabPulsarRegistryUtils` runs two publisher passes (event contracts +
  job contracts) over one shared `CodelabTopicRegistry`, and scans both consumer contracts
  when building endpoints; `CodelabPulsarListenerConfigurer` picks the dispatcher per
  contract.

### 7.3 Message size caps

- **App level**, enforced in the publisher handlers *before* the producer is touched
  (`MessageSizeGuard` → `CodelabMessageSizeExceededException`): keys
  `app.pulsar.events.max-message-size` (default `1KB`) and `app.pulsar.jobs.max-message-size`
  (default `100KB`) in the megalith `application.yaml`, parsed with Spring's `DataSize`.
- **Infra level:** Pulsar's `maxMessageSize` is broker-wide (no per-topic override), so
  docker-compose sets `PULSAR_PREFIX_maxMessageSize=1048576` (**1MB**) via the existing
  `apply-config-from-env.py` step (the key is absent from `standalone.conf`; the script adds
  missing keys). This also bounds what Debezium can write — a full `orders` row is far below
  it.

### 7.4 Topic kind prefixes: events and jobs are structurally disjoint

Before this fork the two buses shared one naming scheme — `event.created-orders` and a job
`created-orders` would have resolved to the *same* physical topic inside a module's tenant.
Now every physical topic carries its kind as a name prefix, applied by
`CodelabTopicResolver` at **both** ends: the publish path (annotation scans in
`CodelabPulsarRegistryUtils`) and the consume path (endpoint resolution in
`CodelabPulsarListenerConfigurer`) go through the same kind-aware resolver, so producer and
consumer can never disagree.

- Config: `app.pulsar.events.topic-prefix` (default `event`) and
  `app.pulsar.jobs.topic-prefix` (default `job`) in the megalith `application.yaml`.
- A **blank** configured value disables prefixing — the escape hatch for topics produced
  outside the framework.
- Physical names today: `persistent://codelab-orders/local/event.created-orders`,
  `.../job.pending-movies`, `.../job.orders-cdc.public.orders`. The logical names written
  in annotations are unchanged (no `event.`/`job.` in source).
- Debezium's `topic.prefix` is set to `job.orders-cdc`, so its produce-side
  `<topic.prefix>.<schema>.<table>` matches the consume-side resolution of
  `@CodelabJobSubscription(topic = "orders-cdc.public.orders")`.
- DLQ names derive from the resolved topic, so they inherit the prefix automatically
  (e.g. `job.pending-movies-movies.movies-processor-DLQ`).