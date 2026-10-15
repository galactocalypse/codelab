### Codelab Megalith

Megalith is the module container within the Codelab ecosystem.
It directly manages all infra dependencies, discovers CodelabModules registered via Spring factories, 
and includes their autoconfiguration within its application context.


### Module Registry

`CodelabModuleRegistrar` is responsible for discovering modules and triggering infra-specific bean registration for 
all infra components for all modules.

### Persistence

Megalith exposes JPA semantics as-is at a per-module level. It manages connectivity configuration to a common data
source, and allows modules to connect to separate logical units within the database. Each module is mapped 

Megalith disables JPA autoconfiguration. Instead, it provides module-specific DataSource, EntityManagerFactory, 
and TransactionManager for a given persistence unit, supplied via module config. It also instantiates all JpaRepository
extensions found within the module.

Per-module wiring specifics:

- `MegalithApplication` excludes `DataSourceAutoConfiguration`, `HibernateJpaAutoConfiguration`, and
  `DataJpaRepositoriesAutoConfiguration`. Each module registers its own EntityManagerFactory, transaction manager,
  and repositories via `CodelabJpaRegistryUtils` — no Boot autoconfiguration is involved.
- The base `spring.datasource.url` in `application.yaml` (`.../codelab`) is only a template.
  `CodelabDataSourceFactory` swaps the database name per module (`JdbcUrls.withDatabase(baseUrl, module.databaseName())`),
  so all modules share one Postgres server but target separate logical databases. For example the TMDB data lives in
  the **`movies`** database, not `codelab`.
- Transaction managers are bean-named per module — `<module>.transactionManager` (e.g. `movies.transactionManager`,
  `orders.transactionManager`) — and none is `@Primary`, so `@Transactional` annotations must qualify explicitly.
- Transactions are active only because `MegalithConfiguration` declares `@EnableTransactionManagement`. Boot's own
  `TransactionAutoConfiguration` backs off here (DataSource autoconfiguration is excluded); without that annotation,
  `@Transactional` is silently a no-op. This was a real production bug: it surfaced as `LazyInitializationException`
  during TMDB re-imports (every repository method self-transacted and returned detached entities).
- `CodelabModuleRegistrar` discovers JPA entities and repositories by scanning the module base package
  (`com.codelab.movies.tmdb.entity` / `.repository`). **Services are not auto-registered** — a module must
  `@Import` its services explicitly in its autoconfiguration (`MoviesAutoConfiguration` uses an explicit `@Import`
  list).

### Messaging

Megalith uses Apache Pulsar for messaging, exposing thin interfaces that piggyback on Spring Boot's Pulsar starter
implementation. Each module is mapped to a dedicated Pulsar tenant. Topics and subscriptions are internally
namespaced to the module, and every physical topic carries a kind prefix — `event.<name>` or `job.<name>`
(`app.pulsar.events.topic-prefix` / `app.pulsar.jobs.topic-prefix`, applied by `CodelabTopicResolver` on both the
publish and consume paths, so the two buses are structurally disjoint inside a module's tenant).

There are two buses with different contracts (full design in `../MESSAGE_EVOLUTION.md`):

**Events** — business facts, governed by the business-version contract. Producers are per type, as opposed to per
queue, mimicking JPA's repository pattern, and every publish mandates a `BusinessVersion`:
```java
@CodelabTopic("created-orders")
public interface OrderCreatedEventPublisher extends CodelabEventPublisher<OrderCreatedEvent> {
}
```
```java
createdPublisher.publish(orderId, BusinessVersion.of("v1"), OrderCreatedEvent.builder().orderId(orderId).build());
```
No service in this repo publishes domain events directly: CDC does. Debezium Server (compose profile `debezium`)
captures the `orders` table via logical replication onto `persistent://codelab-orders/local/job.orders-cdc.public.orders`
(the `job.` prefix comes from `topic.prefix` in the Debezium config, mirroring `app.pulsar.jobs.topic-prefix`),
and `OrderCdcNormalizer` turns row changes into version-stamped events — `c` → `OrderCreatedEvent` (keyed),
`u` → `OrderUpdatedEvent`, everything else ignored. This also means ordering is handled by the DB (a status update
published *after* the flush), and `OrderServiceImpl` has zero messaging knowledge.

Consumers declare which versions they support; the framework checks the version **before** deserializing the payload
and routes unsupported messages straight to the DLQ (bypassing nack/retry):
```java
@CodelabSubscription(topic = "created-orders", subscriptionName = "created-orders-logger", concurrency = 1)
public class OrderCreatedEventProcessor implements CodelabEventConsumer<OrderCreatedEvent> {

  @Override
  public Set<BusinessVersion> getSupportedBusinessVersions() {
    return Set.of(BusinessVersion.of("v1"), BusinessVersion.LEGACY);
  }

  @Override
  public void consume(OrderCreatedEvent event) {
    // business logic
  }
}
```

**Jobs** — heavy async units of work (file ids, CDC envelopes). Same producer/consumer shape, but **no business
version** anywhere: `CodelabJobPublisher.publish(T)`, `CodelabJobConsumer.consume(T)`, annotations
`@CodelabJobTopic` / `@CodelabJobSubscription`. The dispatcher (`CodelabJobMessageDispatcher`) only decodes and
delegates — any failure propagates, so Pulsar's native nack → redelivery → `DeadLetterPolicy` → DLQ handles it:
```java
@CodelabJobTopic("pending-movies")
public interface MovieJobPublisher extends CodelabJobPublisher<MovieJob> {
}
```
```java
@CodelabJobSubscription(
    topic = "pending-movies",
    subscriptionName = "movies-processor",
    initialPosition = SubscriptionInitialPosition.Earliest,
    concurrency = 4,
    deadLetterPolicyRef = "movieDeadLetterPolicy")
public class MovieProcessor implements CodelabJobConsumer<MovieJob> { ... }
```

**Message size caps.** Publish handlers enforce app-level caps before the producer is touched
(`MessageSizeGuard` → `CodelabMessageSizeExceededException`): `app.pulsar.events.max-message-size` (default 1KB)
and `app.pulsar.jobs.max-message-size` (default 100KB) in `application.yaml`. Pulsar's `maxMessageSize` is
broker-wide (no per-topic override), so compose additionally sets `PULSAR_PREFIX_maxMessageSize=1048576` (1MB)
as the hard infra ceiling.

**End-to-end test.** `CdcJobEventEndToEndTest` (`src/test/.../e2e`) proves the whole CDC → event path against a
real broker via Testcontainers `PulsarContainer`: a Debezium-shaped insert envelope is seeded onto
`job.orders-cdc.public.orders`, the real `OrderCdcNormalizer` job consumer turns it into a version-stamped
`OrderCreatedEvent` through the real publisher proxy, and a recording event consumer observes it on
`event.created-orders`. It boots `CdcEndToEndApplication` — the messaging wiring without JPA (the module
registrar is restricted to publishers/subscriptions) — and provisions the module tenant/namespace in the
container, since a fresh broker starts with only `public/default`. Debezium itself is out of the loop (the
envelope is seeded directly), keeping the test fast and deterministic; it is skipped when Docker is absent.

### Headless tasks

Modules can expose **headless entry points** — operator/dev-invoked one-shot units of work (imports, feeds,
backfills, diagnostics, admin commands) — as `CodelabTask`s. A task is `int run() throws Exception` marked with
`@CodelabTaskSpec("name")` and lives under the module base package; `CodelabModuleRegistrar` discovers it with the
same per-module scan used for topics/subscriptions and registers it as a **lazy** singleton bean. No task is
instantiated at boot.

A single dispatcher, `CodelabTaskRunner`, owns selection via one namespaced selector:

| Invocation | Behavior |
|---|---|
| (no selector) | Normal web boot; nothing runs |
| `--codelab.task=list` | List every registered task (name, module, description) |
| `--codelab.task=<name>` | Run exactly that task and exit with its code (2 if unknown, 1 if it throws) |

`MegalithApplication.main` detects the selector, forces `WebApplicationType.NONE`, and exits via
`SpringApplication.exit(...)` with the task's code — so the one-shot/exit plumbing is centralized. Only ids are
published by the movie feeder (never parsed) and the diagnostic touches neither queue nor DB.

Invocation:

```bash
./gradlew bootRun --args='--codelab.task=list'
./gradlew bootRun --args='--codelab.task=movies.feed'
./gradlew bootRun --args='--codelab.task=movies.import'
```

The abstraction is deliberately narrow: it is for a finite set of named, manually-invoked, one-at-a-time units of
work that run against the application context — **not** continuous consumers, `@Scheduled` jobs, or request-path
work (use `CodelabJobConsumer`/`CodelabEventConsumer`, Spring scheduling, or a REST controller respectively). See
`HEADLESS_ENTRY_POINTS.md` at the repo root for the problem, rationale, and applicability table.
The application-driven job bus is verified separately by `JobBusEndToEndTest`: it manually publishes through
the real job-publisher proxy and asserts a real job consumer receives it (jobs are never produced by CDC).

### TMDB ingestion (fan-out ingest)

The bulk TMDB load in `codelab-movies` reuses the messaging stack above as a resumable, idempotent pipeline:

- **Feeder** — `MovieFeedTask` (`--codelab.task=movies.feed`) streams `*.json` file names from
  `${movies.directory}` and publishes one `MovieJob(id)` per file, keyed by the TMDB id, onto the
  `job.pending-movies` job topic. It is a headless task (see "Headless tasks" below), so a normal web
  boot never feeds; `movies.feed.limit=N` gates pilots. Only ids are published (never parsed), so
  memory is bounded by the ~60 MB topic rather than the 35 GB corpus.
- **Consumer** — `MovieProcessor` (a `CodelabJobConsumer`) subscribes as `movies.movies-processor` with
  `initialPosition=Earliest`, `Key_Shared`, `concurrency=4`, and a `DeadLetterPolicy` (`maxRedeliverCount=3`) referencing DLQ topic
  `job.pending-movies-movies.movies-processor-DLQ`. It resolves `${movies.directory}/<id>.json`, skips missing files,
  `{}` 404-markers, `id == 0`, and unparseable JSON (acked, never redelivered); everything else is parsed and handed
  to `TmdbMovieImportService.persist` — one JPA transaction per message.
- **Persistence is idempotent** — natural TMDB ids as `@Id` (no `@GeneratedValue`); `persist` branches on
  `findById` to insert or update in place, so re-runs and duplicates converge. Reference data resolves through
  bounded in-JVM caches; people use `getReferenceById` lazy proxies to avoid a SELECT per cast/crew row. Re-imports
  assign fresh `@ManyToMany` sets (composite-PK join tables) while clearing in place the `orphanRemoval` `@OneToMany`
  children — see `codelab-movies/INGESTION_PIPELINE.md` for why.
- **`Earliest` is load-bearing** — the `@CodelabJobSubscription` wiring default (Latest) would make a recreated
  subscription silently skip a re-feed.
- **Offline diagnostic** — `MovieImportTask` (`--codelab.task=movies.import`) parses `${movies.directory}`
  in-process and prints a genre histogram, touching neither the queue nor the DB. Like the feeder it is a
  library class in `codelab-movies`, so its invocation context is always this module: run it with
  `bootRun --args='--codelab.task=movies.import'`. See `codelab-movies/INGESTION_PIPELINE.md` for the
  full command.
