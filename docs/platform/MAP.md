# Platform Map

Auto-generated-ish reference (refresh with `/refresh-map`). Documents modules, their public
APIs, messaging topics/events, headless tasks, persistence tables, and extension points.

## Modules

| Module | DB | Persistence unit | Base package | Role |
|---|---|---|---|---|
| `codelab-commons` | n/a | n/a | `com.codelab.common` | Shared SPI/contracts: module API, event/job buses, `BusinessVersion`, `CodelabTask` SPI. Published to Maven Local. |
| `codelab-movies` | `movies` | `moviesPU` | `com.codelab.movies` | TMDB persistence + resumable Pulsar ingest pipeline. |
| `codelab-orders` | `orders` | `ordersPU` | `com.codelab.orders` | Order lifecycle CRUD + Debezium CDC -> versioned events pipeline. |
| `codelab-sample` | `sample` | `samplePU` | `com.codelab.sample` | Tiny reference module for all megalith features. |
| `codelab-todos` | `todos` | `todosPU` | `com.codelab.todos` | Todo lists + items (deadline per list, notes per item, done/undone toggling). |
| `codelab-megalith` | n/a (container) | n/a | `com.codelab.megalith` | Running app; per-module wiring; owns infra. |

## Shared/infrastructure

- All modules share one Postgres server but target **separate logical databases** (`movies`, `orders`,
  `sample`, `todos`). There are no cross-module shared tables.
- Pulsar is shared infra; modules map to dedicated tenants (`codelab-<module>`) and topics are
  kind-prefixed (`event.<name>` / `job.<name>`) in namespace `local`.
- Transaction managers are bean-named `<module>.transactionManager` and none is `@Primary`.

## Module public APIs

### `codelab-orders` (REST)

Base paths and service methods:

- `ProductController` -> `/api/products`
  - `ProductService.createProduct(CreateProductRequest) -> CreateProductResponse`
  - `ProductService.getProduct(Long) -> GetProductResponse`
- `CustomerController` -> `/api/customers`
  - `CustomerService.createCustomer(CreateCustomerRequest) -> CreateCustomerResponse`
  - `CustomerService.getCustomer(Long) -> GetCustomerResponse`
- `OrderController` -> `/api/orders`
  - `OrderService.createOrder(CreateOrderRequest) -> CreateOrderResponse`
  - `OrderService.getOrder(Long) -> GetOrderResponse`
  - `OrderService.updateStatus(Long, UpdateOrderRequest) -> UpdateOrderResponse`

### `codelab-sample` (REST + service)

- `SampleController` -> `/api/samples`
  - `SampleService.createSample(CreateSampleRequest) -> CreateSampleResponse`
  - `SampleService.getSample(Long) -> GetSampleResponse`
  - `SampleService.submitForProcessing(Long) -> void`

### `codelab-movies`

- No public REST API (ingestion via Pulsar pipeline and headless tasks).
- `TmdbMovieImportService.persist(MovieDetails) -> boolean` (transactional, `movies.transactionManager`).

### `codelab-todos` (REST)

- `TodoController` -> `/api/todos`
  - `TodoService.createTodoList(CreateTodoListRequest) -> CreateTodoListResponse` (`POST /api/todos`)
  - `TodoService.getTodoList(Long) -> GetTodoListResponse` (`GET /api/todos/{id}`, includes items)
  - `TodoService.listTodoLists() -> List<TodoListSummaryResponse>` (`GET /api/todos`)
  - `TodoService.addTodoItem(Long, AddTodoItemRequest) -> GetTodoListResponse` (`POST /api/todos/{id}/items`)
  - `TodoService.updateTodoItem(Long, Long, UpdateTodoItemRequest) -> GetTodoListResponse`
    (`PUT /api/todos/{id}/items/{itemId}`; partial: description, notes, done)
  - `TodoService.toggleTodoItem(Long, Long, boolean) -> GetTodoListResponse`
    (`PATCH /api/todos/{id}/items/{itemId}/done` and `.../undone`)
- Unknown list/item ids throw `TodoNotFoundException` -> HTTP 404.
- No auth/user management; lists own their items (`orphanRemoval`, ordered by id).

## Messaging topics and events

### Events bus (versioned)

| Module | Topic (logical) | Physical | Publisher | Consumer(s) | Versions |
|---|---|---|---|---|---|
| orders | `created-orders` | `.../event.created-orders` | `OrderCreatedEventPublisher` | `OrderCreatedEventProcessor` (subscription `created-orders-logger`), `OrderCdcNormalizer` indirectly | `v1`, `LEGACY` |
| orders | `updated-orders` | `.../event.updated-orders` | `OrderUpdatedEventPublisher` | `OrderUpdatedEventProcessor` (subscription `updated-orders-logger`) | `v1`, `LEGACY` |
| sample | `created-samples` | `.../event.created-samples` | `SampleCreatedEventPublisher` | `SampleCreatedEventProcessor` (subscription `created-samples-logger`) | `v1`, `LEGACY` |

Events payloads: `OrderCreatedEvent`, `OrderUpdatedEvent`, `SampleCreatedEvent`
(`@PulsarMessage(schemaType = JSON)`).

### Jobs bus (versionless)

| Module | Topic (logical) | Physical (example) | Publisher | Consumer | Notes |
|---|---|---|---|---|---|
| movies | `pending-movies` | `.../job.pending-movies` | `MovieJobPublisher` (`MovieFeedTask`) | `MovieProcessor` (`movies-processor`, Earliest, Key_Shared, concurrency=4, DLQ) | `MovieJob { id }` |
| orders (CDC) | `orders-cdc.public.orders` | `.../job.orders-cdc.public.orders` | Debezium Server (compose) | `OrderCdcNormalizer` -> publishes events | DLQ `orderCdcDeadLetterPolicy` |
| sample | `pending-samples` | `.../job.pending-samples` | `SampleJobPublisher` (`SampleFeedTask`, `submitForProcessing`) | `SampleJobProcessor` (`samples-processor`, Earliest, Key_Shared, concurrency=2, DLQ) | `SampleJob { sampleId }` |

See `MESSAGE_EVOLUTION.md` for version/DLQ/size-cap details.

## Headless tasks (extension points for operators)

| Module | Task name | Implementation | Description |
|---|---|---|---|
| movies | `movies.feed` | `MovieFeedTask` | Publish movie ids from `${movies.directory}`. |
| movies | `movies.import` | `MovieImportTask` | Parse directory in-process, print genre histogram. |
| sample | `sample.ping` | `SamplePingTask` | Count `sample_items` rows. |
| sample | `sample.feed` | `SampleFeedTask` | Publish a `SampleJob` per `sample_items` row. |

Run via: `cd codelab-megalith && ./gradlew bootRun --args='--codelab.task=<name>'`.

## Persistence tables (owned per module)

### `movies` (database)
- `tmdb_collections`, `tmdb_companies`, `tmdb_countries`, `tmdb_genres`, `tmdb_keywords`,
  `tmdb_languages`, `tmdb_movie_cast`, `tmdb_movie_crew`, `tmdb_movies`, `tmdb_people`,
  `tmdb_reviews`.

### `orders` (database)
- `products`, `customers`, `orders`, `line_items`.

### `sample` (database)
- `sample_items`.

### `todos` (database)
- `todo_lists`, `todo_items` (`todo_items.todo_list_id` FK; cascading delete via `orphanRemoval`).

## Extension points for new modules

1. Implement `CodelabModuleProvider` (Spring factory SPI `com.codelab.common.spring.persistence.CodelabModuleProvider`).
2. Return `new CodelabModule(name, basePackage, persistenceUnit, databaseName, overrides)`.
3. Provide an `@AutoConfiguration` class that `@Import`s controllers/services and defines any module
   `DeadLetterPolicy` beans.
4. Register resources under `META-INF/spring/`:
   - `org.springframework.boot.autoconfigure.AutoConfiguration.imports`
   - `spring.factories` mapping `CodelabModuleProvider`.
5. Add module dependency to `codelab-megalith/build.gradle`.
6. Follow naming conventions: transactions `<module>.transactionManager`, topics `event.<name>`/`job.<name>`.

Module-level extension SPIs (in `codelab-commons`):
- `CodelabModuleProvider` (module registration)
- `CodelabEventPublisher<T>` / `CodelabEventConsumer<T>` + `@CodelabTopic` / `@CodelabSubscription`
- `CodelabJobPublisher<T>` / `CodelabJobConsumer<T>` + `@CodelabJobTopic` / `@CodelabJobSubscription`
- `CodelabTask` + `@CodelabTaskSpec`

## Platform wiring notes

- `MegalithApplication` discovers modules via `SpringFactoriesLoader` + `CodelabModuleRegistrar`.
- `@Transactional` must specify `transactionManager = "<module>.transactionManager"`.
- Publishing a domain event/ job requires obtaining the per-type publisher interface injected by the framework.
- Headless tasks are lazily registered singleton beans and only instantiated when selected.

Refresh: `/refresh-map`.
