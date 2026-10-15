# Codelab

Polyglot demo/workspace repo. The backend is a **Spring Boot "megalith"** that hosts multiple
`CodelabModule`s (each contributing its own JPA persistence unit, Pulsar topics/subscriptions, and
REST controllers), with a generated TypeScript frontend.

## Repository layout

There is **no root Gradle wrapper** — every `codelab-*` directory is its own standalone Gradle build
with its own `gradlew`/`settings.gradle`. Run Gradle commands *inside* the module directory.

| Directory | Role |
|---|---|
| `codelab-gradle` | Shared build plugin (`com.codelab.gradle`) + settings plugin (`com.codelab.settings`). Enforces Gradle 9.7.1 / Java 25, `com.codelab` group prefix, maven-publish conventions, repo setup. |
| `codelab-bom` | `java-platform` BOM of dependency versions (Spring Boot 4.1.1, Lombok, etc.). |
| `codelab-commons` | Shared library: module SPI, event/job bus contracts, `BusinessVersion`, `CodelabTask` SPI. |
| `codelab-megalith` | The running application / module container. Owns infra wiring: per-module DataSource/EntityManagerFactory/TransactionManager/Pulsar registration. **Start here.** |
| `codelab-movies` | `CodelabModule` for movies; TMDB persistence layer and Pulsar ingestion pipeline. |
| `codelab-orders` | `CodelabModule` for orders; demo CRUD plus CDC -> event pipeline. |
| `codelab-sample` | Tiny reference module demonstrating every megalith feature (dynamic data source, events, jobs, manual tasks). **Read this first as an agent.** |
| `frontend` | Generated TypeScript/OpenAPI client (React + Vite). |
| `docker-compose.yml` | Local infra; containers gated behind `--profile` flags (postgres, pulsar, pulsar-manager, redis, cassandra, neo4j, debezium). |

## Build & test

- **Java 25, Gradle 9.7.1** (enforced by `com.codelab.gradle`; a different version fails the build). Always use the bundled `./gradlew` inside the module.
- **Run `./gradlew spotlessApply` before `check`** — Spotless uses `googleJavaFormat()` and plain
  `check` fails on unformatted code.
- Modules publish to **mavenLocal** and the megalith resolves them from there: after touching any
  module, run `./gradlew publishToMavenLocal` or the megalith silently runs stale code. Publish in
  dependency order (`codelab-gradle`, `codelab-bom`, `codelab-commons`, then modules).
- Verification targets:
  ```bash
  ./gradlew spotlessApply check        # inside codelab-movies (22 tests incl. Testcontainers Postgres)
  ./gradlew spotlessApply check        # inside any module
  ./gradlew spotlessApply check        # inside codelab-megalith (compiles the whole app)
  ./gradlew bootRun                    # inside codelab-megalith — runs the app on :8080
  ```

## Key architecture facts

- `MegalithApplication` excludes `DataSourceAutoConfiguration`, `HibernateJpaAutoConfiguration`, and
  `DataJpaRepositoriesAutoConfiguration`; wiring happens through `CodelabModuleRegistrar`.
- Each module gets its own EMF, transaction manager (`<module>.transactionManager`, e.g.
  `movies.transactionManager`), and repositories via `CodelabJpaRegistryUtils`; none is `@Primary`, so
  `@Transactional` must qualify the transaction manager explicitly.
- Transactions only work because `MegalithConfiguration` declares `@EnableTransactionManagement`.
- `spring.datasource.url` is only a template; `CodelabDataSourceFactory` swaps the DB name per module.
  TMDB data lives in the **`movies`** database, not `codelab`.
- Services are not auto-registered; modules `@Import` them explicitly in their autoconfiguration.
- Messaging uses Apache Pulsar: per-module tenant, kind-prefixed topics (`event.<name>` /
  `job.<name>`). Events are versioned (`BusinessVersion`); jobs are not and use native retry/DLQ.
- No service publishes domain events directly: CDC (Debezium) drives event emission.
- Headless `CodelabTask`s run via `--codelab.task=<name>`; use `--codelab.task=list` to survey them.
- The IJ/opencode LSP frequently reports bogus Lombok diagnostics (`getId()` undefined, `log cannot be
  resolved`, "blank final field") — **Gradle is the source of truth**, not the language server.

## Runtime notes

- Start infra with profile flags, e.g.
  `docker compose --profile postgres --profile pulsar up -d` (add `--profile debezium` for CDC).
- App serves Swagger UI at `/swagger-ui` and OpenAPI JSON at `/api-docs`.
- Browser-visible manual commands live in `HEADLESS_ENTRY_POINTS.md`; TMDB pipeline specifics live in
  `codelab-movies/INGESTION_PIPELINE.md`.

## Documentation index

| Doc | What it covers |
|---|---|
| `README.md` | Comprehensive project README (getting started, publish order, architecture, frontend). |
| `docs/platform/MAP.md` | Platform map (modules, public APIs, events/topics, tasks, tables, extension points). Load this first instead of broad repo scans. |
| `codelab-megalith/PROJECT.md` | Megalith architecture: module registry, per-module persistence wiring, messaging, TMDB fan-out. |
| `MESSAGE_EVOLUTION.md` | Event vs job bus design, business-version contract, CDC, DLQ routing, size caps. |
| `HEADLESS_ENTRY_POINTS.md` | Rationale and design of the `CodelabTask` SPI. |
| `codelab-movies/INGESTION_PIPELINE.md` | End-to-end TMDB ingestion runbook, reset & monitoring commands, pitfalls. |
| `codelab-gradle/README.md` | Build plugin/settings plugin conventions and usage. |
| `codelab-orders/README.md` | Orders module. |
| `codelab-sample/README.md` | Reference module walkthrough. |
| `frontend/README.md` | Generated OpenAPI client. |
| `codelab-movies/HELP.md`, `codelab-megalith/HELP.md` | spring-initializr boilerplate. |
| `AGENTS.md` | This file: contributor notes for AI agents. |

## Agent navigation

- **Load and rely on `docs/platform/MAP.md` first** instead of scanning the full repository; it is
  the platform map for modules, public APIs, events/topics, tasks, tables, and extension points.
- Use `/refresh-map` (or edit the file directly) to regenerate it when it drifts from code.

## Finalization workflow

- Once changes are finalized in a session and are about to be committed/pushed, regenerate/update
  `docs/platform/MAP.md` so the committed state reflects the latest modules/APIs/topics.
- Include the refreshed map in the same commit (or immediately in the next one).

## Agent guidance

- Prefer explicit transaction-manager qualifiers and per-module repositories when adding persistence.
- After changing a module, always `publishToMavenLocal` before building/running the megalith.
- Format with `spotlessApply` before `check`.
- Do not trust LSP diagnostics; use Gradle as the source of truth.
- Read `codelab-sample` first when learning how a module contributes wiring.