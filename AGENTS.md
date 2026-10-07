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
| `codelab-bom` | `java-platform` BOM of dependency versions. |
| `codelab-commons` | Shared library module. |
| `codelab-megalith` | The running application / module container. Owns infra wiring: per-module DataSource/EntityManagerFactory/TransactionManager/Pulsar registration. **Start here.** |
| `codelab-movies` | `CodelabModule` for movies; holds the TMDB persistence layer and the Pulsar ingestion pipeline. |
| `codelab-orders` | `CodelabModule` for orders (demo CRUD + event publisher/consumer patterns). |
| `frontend` | Generated TypeScript/OpenAPI client for the megalith's REST API. |
| `docker-compose.yml` | Local infra; containers are gated behind `--profile` flags (postgres, pulsar, pulsar-manager, redis, cassandra, neo4j). |

## Build & test

- **Java 25, Gradle 9.7.1** (enforced by the `com.codelab.gradle` plugin; a different version fails the build).
- **Run `./gradlew spotlessApply` before `check`** — Spotless uses `googleJavaFormat()` and plain
  `check` fails on unformatted code.
- codelab-movies is published to **mavenLocal** and resolved from there by the megalith: after touching
  `codelab-movies`, run `./gradlew publishToMavenLocal` or the megalith silently runs stale code.
- Verification targets:
  ```bash
  ./gradlew spotlessApply check        # inside codelab-movies (22 tests incl. Testcontainers Postgres)
  ./gradlew spotlessApply check        # inside codelab-megalith (compiles the whole app)
  ./gradlew bootRun                    # inside codelab-megalith — runs the app on :8080
  ```

## Key architecture facts

- `MegalithApplication` **excludes** `DataSourceAutoConfiguration`, `HibernateJpaAutoConfiguration`,
  and `DataJpaRepositoriesAutoConfiguration`. Each module registers its own EMF, transaction manager
  (`<module>.transactionManager`, e.g. `movies.transactionManager`), and repositories via
  `CodelabJpaRegistryUtils`/`CodelabModuleRegistrar`.
- `@Transactional` annotations must **qualify the transaction manager explicitly** (none is `@Primary`),
  and transactions only work because `MegalithConfiguration` declares `@EnableTransactionManagement`
  (Boot's `TransactionAutoConfiguration` backs off here).
- The base `spring.datasource.url` is only a template — `CodelabDataSourceFactory` swaps the DB name per
  module. The TMDB data lives in the **`movies`** database (not `codelab`).
- Services are **not** auto-registered; modules `@Import` their services in their autoconfiguration
  (`MoviesAutoConfiguration` uses an explicit `@Import` list).
- Messaging: each module maps to its own Pulsar tenant; producers are per-type interfaces
  (`@CodelabTopic`), consumers via `@CodelabSubscription`. Details in `codelab-megalith/PROJECT.md`.
- The IJ/opencode LSP frequently reports bogus Lombok diagnostics (`getId()` undefined, `log cannot be
  resolved`, "blank final field") — **Gradle is the source of truth**, not the language server.

## Documentation

| Doc | What it covers |
|---|---|
| `codelab-megalith/PROJECT.md` | Megalith architecture: module registry, per-module persistence wiring, messaging, and the TMDB fan-out ingest. |
| `codelab-movies/INGESTION_PIPELINE.md` | End-to-end TMDB ingestion runbook: feed → Pulsar → consumer → idempotent persist, reset & monitoring commands, measured profile, known pitfalls. |
| `codelab-gradle/README.md` | Build plugin/settings plugin conventions and usage. |
| `codelab-orders/README.md` | Orders module. |
| `frontend/README.md` | Generated OpenAPI client. |
| `codelab-movies/HELP.md`, `codelab-megalith/HELP.md` | spring-initializr boilerplate. |