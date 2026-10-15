# Codelab

A polyglot demo/workspace repo centered on a Spring Boot **"megalith"**: a modular monolith that
hosts multiple self-describing `CodelabModule`s, each contributing its own JPA persistence unit,
Apache Pulsar topics/subscriptions, and REST controllers. A generated TypeScript/OpenAPI client
lives in `frontend/`.

## Concepts

| Concept | What it is |
|---|---|
| **Megalith** (`codelab-megalith`) | The one running application. It owns all infrastructure wiring and the Spring `ApplicationContext`. |
| **Module** (`codelab-movies`, `codelab-orders`) | A `java-library` (no `main`) that declares itself through the `CodelabModuleProvider` Spring-factory SPI and contributes entities, repositories, services, controllers, topics, consumers, and tasks. |
| **Events bus** | Versioned business facts. Producers require a `BusinessVersion`; consumers declare supported versions; unsupported messages are routed straight to the DLQ. |
| **Jobs bus** | Version-less, heavy async work (file ids, CDC envelopes) with native nack → retry → `DeadLetterPolicy` semantics. |
| **Headless task** | An operator/dev-invoked one-shot unit of work (`CodelabTask`), selected with `--codelab.task=<name>`. |
| **Codelab Gradle plugin** (`codelab-gradle`) | Shared build conventions (`com.codelab.gradle`) and dependency-resolution conventions (`com.codelab.settings`). |

## Repository layout

There is **no root Gradle build** — every `codelab-*` directory is its own standalone Gradle build
with its own `gradlew`/`settings.gradle`. Run Gradle from *inside* the module directory.

| Directory | Role |
|---|---|
| `codelab-gradle` | Shared build plugin + settings plugin. Enforces Gradle 9.7.1 / Java 25, the `com.codelab` group prefix, and Maven publishing. |
| `codelab-bom` | `java-platform` BOM of dependency versions (Spring Boot 4.1.1, Lombok, etc.). |
| `codelab-commons` | Shared library: module SPI, event/job bus contracts, `BusinessVersion`, and the `CodelabTask` SPI. |
| `codelab-megalith` | The running application / module container. Owns per-module DataSource/EntityManagerFactory/TransactionManager/Pulsar registration. **Start here.** |
| `codelab-movies` | `CodelabModule` for movies: TMDB persistence layer and the Pulsar ingestion pipeline. |
| `codelab-orders` | `CodelabModule` for orders: demo CRUD plus the CDC → event pipeline. |
| `codelab-todos` | `CodelabModule` for todos: todo lists with a deadline, items with notes, and done/undone toggling (no auth). |
| `frontend` | Generated TypeScript/OpenAPI client (React + Vite) for the megalith's REST API. |
| `docker-compose.yml` | Local infra; containers are gated behind `--profile` flags. |

## Requirements

- **Java 25** and **Gradle 9.7.1** — both are enforced by `com.codelab.gradle`; a different version
  fails the build. Always use the bundled `./gradlew`.
- **Docker + Docker Compose** for local infrastructure.
- **Node.js** (for the `frontend` only).

## Getting started

### 1. Start infrastructure

Containers are opt-in via profiles:

```bash
docker compose --profile postgres --profile pulsar up -d
# optional: --profile debezium --profile pulsar-manager --profile redis --profile cassandra --profile neo4j
```

Postgres runs with `wal_level=logical` so Debezium (profile `debezium`) can stream the `orders`
table via logical replication.

### 2. Publish shared artifacts to Maven Local

Each module resolves the others (and the Gradle plugins) from **Maven Local**, so they must be
published in dependency order first:

```bash
cd codelab-gradle   && ./gradlew publishToMavenLocal   # com.codelab.gradle / com.codelab.settings
cd ../codelab-bom   && ./gradlew publishToMavenLocal
cd ../codelab-commons && ./gradlew publishToMavenLocal
cd ../codelab-movies  && ./gradlew spotlessApply check publishToMavenLocal
cd ../codelab-orders  && ./gradlew spotlessApply check publishToMavenLocal
```

> The versioned plugins in each `settings.gradle` (`id 'com.codelab.settings' version '1.0.0'`)
> resolve from Maven Local because `pluginManagement` includes `mavenLocal()` (this repo relies on a
> `~/.gradle/init.d/` init script to add it). Publish `codelab-gradle` at least once before building
> anything else.

After changing a module, **re-run `publishToMavenLocal`** or the megalith silently runs stale code.

### 3. Run the application

```bash
cd codelab-megalith
./gradlew bootRun          # http://localhost:8080  (Swagger UI at /swagger-ui, JSON at /api-docs)
```

## Architecture highlights

### Modular monolith

`MegalithApplication` discovers `CodelabModule`s via `SpringFactoriesLoader` and
`CodelabModuleRegistrar` triggers per-module bean registration (JPA, Pulsar publishers/subscriptions,
and tasks). Each module exposes its autoconfiguration through
`META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` and **explicitly
`@Import`s its services** (services are not auto-registered).

### Per-module persistence

Boot's `DataSourceAutoConfiguration`, `HibernateJpaAutoConfiguration`, and
`DataJpaRepositoriesAutoConfiguration` are **excluded**. Instead each module gets its own
EntityManagerFactory, transaction manager (`<module>.transactionManager`, e.g.
`movies.transactionManager`), and repositories via `CodelabJpaRegistryUtils`.

- `CodelabDataSourceFactory` rewrites the template `spring.datasource.url` per module, so all
  modules share one Postgres server but target separate logical databases. **TMDB data lives in the
  `movies` database**, not `codelab`.
- No transaction manager is `@Primary`, so `@Transactional` must qualify explicitly. Transactions
  work only because `MegalithConfiguration` declares `@EnableTransactionManagement`.

### Messaging

Every module maps to a dedicated Pulsar tenant, and every physical topic carries a kind prefix so
the two buses are structurally disjoint: `persistent://codelab-<module>/local/event.<name>` or
`.../job.<name>` (configurable via `app.pulsar.events.topic-prefix` / `app.pulsar.jobs.topic-prefix`).

**Events** — business facts, version-gated. Producers are per-type interfaces annotated with
`@CodelabTopic`; publishes mandate a `BusinessVersion`; consumers implement
`CodelabEventConsumer<T>` and return `getSupportedBusinessVersions()`. The framework checks the
version **before deserializing** and routes unsupported messages directly to the DLQ. No service
publishes domain events directly — **CDC does**: Debezium Server captures the `orders` table and
`OrderCdcNormalizer` turns row changes into version-stamped events.

**Jobs** — heavy async work with no business version (`CodelabJobPublisher` / `CodelabJobConsumer`,
`@CodelabJobTopic` / `@CodelabJobSubscription`). Failures use Pulsar's native nack → redelivery →
`DeadLetterPolicy` route.

Publish handlers enforce app-level size caps before hitting the broker
(`app.pulsar.events.max-message-size` = 1KB, `app.pulsar.jobs.max-message-size` = 100KB); compose
sets a 1MB broker-wide ceiling.

### Headless tasks

Modules expose one-shot units of work as `CodelabTask`s annotated with `@CodelabTaskSpec("name")`.
A single dispatcher, `CodelabTaskRunner`, selects exactly one via `--codelab.task`:

| Invocation | Behavior |
|---|---|
| *(no selector)* | Normal web boot; nothing runs |
| `--codelab.task=list` | List every registered task (name, module, description) |
| `--codelab.task=<name>` | Run that task, force headless (`WebApplicationType.NONE`), exit with its code |

```bash
cd codelab-megalith
./gradlew bootRun --args='--codelab.task=list'
./gradlew bootRun --args='--codelab.task=movies.feed'
./gradlew bootRun --args='--codelab.task=movies.import'
```

## Modules

- **Orders** — order lifecycle CRUD (products, customers, orders, line items) with denormalized
  tables and per-action DTOs, plus the Debezium CDC → event pipeline.
- **Movies** — TMDB persistence (`tmdb_*` tables in the `movies` database) and a resumable,
  idempotent fan-out ingestion pipeline: `movies.feed` publishes `MovieJob(id)` per `*.json` file;
  `MovieProcessor` parses and persists one JPA transaction per message. See
  `codelab-movies/INGESTION_PIPELINE.md` for the full runbook.
- **Todos** — todo lists (one shared deadline per list) with items that carry ad-hoc notes and a
  done/undone toggle, exposed at `/api/todos`. No users/auth. See `codelab-todos/README.md`.

## Frontend

The `frontend/` workspace is a React + Vite + TypeScript app. A generated OpenAPI client lives in
`frontend/src/generated` (config: `frontend/openapi-generator.yaml`). The generator reads the live
spec, so **start the backend first, then regenerate the client before building the frontend** —
regenerate whenever a backend REST contract changes, and always before a rebuild so the client
matches the running API.

```bash
# terminal 1 — backend must be up so http://localhost:8080/api-docs is available
cd codelab-megalith && ./gradlew bootRun

# terminal 2
cd frontend
npm install
npm run generate-api   # regenerate frontend/src/generated from the megalith's OpenAPI spec
npm run build          # type-check + production bundle into dist/
npm run dev            # Vite dev server (proxies /api -> :8080)
```

> `npm run generate-api` requires the backend on `:8080`. It is a prerequisite for
> **rebuilding** the frontend, not for running the dev server.

## Verification

Run `spotlessApply` **before** `check` — Spotless uses `googleJavaFormat()` and plain `check` fails
on unformatted code.

```bash
# inside codelab-movies: 22 tests incl. Testcontainers Postgres
./gradlew spotlessApply check

# inside codelab-megalith: compiles the whole app; Testcontainers Pulsar e2e tests
./gradlew spotlessApply check
```

## Documentation

| Doc | What it covers |
|---|---|
| `codelab-megalith/PROJECT.md` | Megalith architecture: module registry, per-module persistence, messaging, headless tasks, TMDB fan-out. |
| `docs/platform/MESSAGE_EVOLUTION.md` | Event vs job bus design, business-version contract, CDC emission, DLQ routing, size caps. |
| `docs/platform/HEADLESS_ENTRY_POINTS.md` | Problem, rationale, and implemented design of the `CodelabTask` SPI. |
| `codelab-movies/INGESTION_PIPELINE.md` | End-to-end TMDB ingestion runbook and reset/monitoring commands. |
| `codelab-gradle/README.md` | Build plugin/settings plugin conventions and usage. |
| `codelab-orders/README.md` | Orders module. |
| `codelab-todos/README.md` | Todos module (lists, items, notes, done/undone). |
| `frontend/README.md` | Generated OpenAPI client. |
| `AGENTS.md` | Contributor notes for AI agents working in this repo. |
