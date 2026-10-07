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
implementation. Each module is mapped to a dedicated Pulsar tenant. Topics and subscriptions are internally namespaced
to the module.

Additionally, producers must be defined per type, as opposed to per queue, mimicking JPA's repository pattern.
For example, to declare a publisher for `CreateOrderEvent`:
```java
@CodelabTopic(name = "orders", value = "orders")
public interface CreateOrderEventPublisher extends CodelabEventPublisher<CreateOrderEvent> {
}
```
Then for actually pushing events:
```java
@Slf4j
@Service
@AllArgsConstructor
public class OrderProducer {

  private final CreateOrderEventPublisher createOrderEventPublisher;

  public void send(CreateOrderRequest request) {
    createOrderEventPublisher.publish(CreateOrderEvent.from(request));
    log.info("Message published at {}", new Date());
  }

}
```
The `CreateOrderEventPublisher` dependency is injected dynamically.

Message consumption works similarly, supporting multiple consumers:
```java
@CodelabSubscription(topic = "orders", subscriptionName = "codelab-orders", concurrency = 1)
public class OrderConsumer implements CodelabEventConsumer<CreateOrderEvent> {

  @Override
  public void consume(CreateOrderEvent event) {
    // business logic
  }
}
```

### TMDB ingestion (fan-out ingest)

The bulk TMDB load in `codelab-movies` reuses the messaging stack above as a resumable, idempotent pipeline:

- **Feeder** — `MovieFeedRunner` streams `*.json` file names from `--movies.directory` and publishes one
  `MovieEvent(id)` per file, keyed by the TMDB id, onto the `pending-movies` topic. It is a gated bean behind
  `movies.feed=true` so a normal web boot never feeds; `--movies.feed.limit=N` gates pilots. Only ids are published
  (never parsed), so memory is bounded by the ~60 MB topic rather than the 35 GB corpus.
- **Consumer** — `MovieProcessor` subscribes as `movies.movies-processor` with `initialPosition=Earliest`,
  `Key_Shared`, `concurrency=4`, and a `DeadLetterPolicy` (`maxRedeliverCount=3`) referencing DLQ topic
  `pending-movies-movies.movies-processor-DLQ`. It resolves `${movies.directory}/<id>.json`, skips missing files,
  `{}` 404-markers, `id == 0`, and unparseable JSON (acked, never redelivered); everything else is parsed and handed
  to `TmdbMovieImportService.persist` — one JPA transaction per message.
- **Persistence is idempotent** — natural TMDB ids as `@Id` (no `@GeneratedValue`); `persist` branches on
  `findById` to insert or update in place, so re-runs and duplicates converge. Reference data resolves through
  bounded in-JVM caches; people use `getReferenceById` lazy proxies to avoid a SELECT per cast/crew row. Re-imports
  assign fresh `@ManyToMany` sets (composite-PK join tables) while clearing in place the `orphanRemoval` `@OneToMany`
  children — see `codelab-movies/INGESTION_PIPELINE.md` for why.
- **`Earliest` is load-bearing** — the `@CodelabSubscription` wiring default (Latest) would make a recreated
  subscription silently skip a re-feed.
