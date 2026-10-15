# codelab-todos

A `CodelabModule` for TODO lists. Deliberately self-contained and auth-free: no
users, no ownership checks, just lists and their items. Useful as a small CRUD
reference (REST + per-module persistence) and as demo data for the frontend.

## Domain

- **Todo list** — a named container with a single `deadline` (`LocalDate`) shared
  by every item in the list.
- **Todo item** — belongs to exactly one list, has a `description`, a free-form
  `notes` field (TEXT, may be added/updated at any time), and a `done` flag.
  Items are owned children (`orphanRemoval`), ordered by id.

## REST API

`TodoController` is mapped at `/api/todos`. Unknown list/item ids return **404**
via `TodoNotFoundException`.

| Method | Path | Purpose |
|---|---|---|
| `GET` | `/api/todos` | List all lists (summary: id, name, deadline, itemCount, doneCount). |
| `POST` | `/api/todos` | Create a list (`name`, optional `deadline`). |
| `GET` | `/api/todos/{id}` | Get one list **with its items**. |
| `POST` | `/api/todos/{id}/items` | Add an item (`description`, optional `notes`). |
| `PUT` | `/api/todos/{id}/items/{itemId}` | Partially update an item (`description`, `notes`, `done`). |
| `PATCH` | `/api/todos/{id}/items/{itemId}/done` | Mark the item done. |
| `PATCH` | `/api/todos/{id}/items/{itemId}/undone` | Mark the item undone. |

Example:

```bash
curl -s -X POST localhost:8080/api/todos \
  -H 'Content-Type: application/json' \
  -d '{"name":"Launch checklist","deadline":"2026-10-20"}'

curl -s -X POST localhost:8080/api/todos/1/items \
  -H 'Content-Type: application/json' \
  -d '{"description":"Write release notes","notes":"include migration steps"}'

curl -s -X PATCH localhost:8080/api/todos/1/items/1/done
```

## Wiring

- `TodosModuleProvider` declares the `todos` database and `todosPU` persistence
  unit; `TodoServiceImpl` uses `@Transactional(transactionManager = "todos.transactionManager")`.
- `TodosAutoConfiguration` `@Import`s the controller and service (services are not
  auto-registered) and is listed in
  `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`.
- The module provider is registered through `META-INF/spring.factories`.
- No Pulsar topics/consumers: the module is REST-only.

## Running

The megalith resolves this module from Maven Local, and it needs its own Postgres
database (like every other module):

```bash
cd codelab-todos
./gradlew spotlessApply check        # unit + Testcontainers Postgres integration tests
./gradlew publishToMavenLocal        # required before building/running codelab-megalith
```

```bash
# one-time: create the logical database on the shared Postgres server
docker exec postgres psql -U postgres -c "CREATE DATABASE todos;"

cd ../codelab-megalith
./gradlew bootRun                    # then use /api/todos (Swagger at /swagger-ui)
```

The frontend surfaces this API at `/todos` (see `frontend/src/pages/todos/`).
