# TMDB Ingestion Pipeline

End-to-end runbook for ingesting a TMDB dump (1.2M+ JSON files, ~35 GB, one file per movie id) into the `movies`
database through the Pulsar fan-out ingest in `codelab-megalith`.

## How it is processed

```
movie_fetcher/data/movies/*.json
   │  (1) MovieFeedRunner  — gated by movies.feed=true; streams *.json filenames, publishes
   │      MovieJob(id) keyed by the TMDB id → job.pending-movies job topic (ids only, never parses)
   ▼
job.pending-movies  (Key_Shared, initialPosition=Earliest)
   │  (2) MovieProcessor  — subscriber movies.movies-processor, concurrency=4, DLQ maxRedeliver=3
   │      reads <dir>/<id>.json; skips missing file / "{}" 404-marker / id==0 / parse failure (acked);
   │      otherwise parses MovieDetails → TmdbMovieImportService.persist
   ▼
tmdb_* tables in the "movies" database
   │  (3) persist is @Transactional("movies.transactionManager"), one tx per message, idempotent:
   │      findById → insert (new) or update-in-place (re-import). Reference data (genres, keywords,
   │      companies, countries, languages, collections) resolved via bounded in-JVM caches; people via
   │      getReferenceById lazy proxies (no SELECT per cast/crew row).
   ▼
errors rethrown → ≤3 redeliveries → DLQ job.pending-movies-movies.movies-processor-DLQ
```

## Prerequisites

- Postgres (`:5432`) and Pulsar (broker `:6650`, admin `:8081`) containers running (`docker compose`).
- `codelab-movies` built and published to mavenLocal — the megalith resolves
  `com.codelab:codelab-movies:1.0.0` from there and will silently run stale code otherwise.
- Corpus directory of `<id>.json` files (default `/home/adarsh/code/movie_fetcher/data/movies`).
- RAM: prefer ≥ 8 GB free before a large run (`bootRun` heap is pinned to 4 g). If swap is hot, stop stray JVMs
  first.

## Build

```bash
cd codelab-movies
./gradlew spotlessApply check        # tests incl. Testcontainers Postgres
./gradlew publishToMavenLocal        # REQUIRED after any codelab-movies change
```

## Run the pipeline

From `codelab-megalith`:

```bash
./gradlew bootRun --args='\
  --movies.feed=true \
  --movies.feed.limit=1000 \
  --movies.directory=/home/adarsh/code/movie_fetcher/data/movies \
  --spring.jpa.show-sql=false \
  --spring.jpa.properties.hibernate.format_sql=false'
```

Run in the background (`nohup ... &`) and tail the log. `show-sql`/`format_sql` default to `true` in
`application.yaml` for dev; on a big ingest they produce millions of log lines — keep them off.

| Flag | Meaning |
|---|---|
| `--movies.feed=true` | run the feeder bean (a normal web boot never feeds) |
| `--movies.feed.limit=N` | feed only the first N `*.json` files (0/default = entire directory) |
| `--movies.directory=...` | corpus root; default points at the real dump |

## Reset runbook

1. **Queue** — this Pulsar build has **no `delete-subscription`**; deleting the topic drops the subscription too:

   ```bash
   docker exec pulsar bin/pulsar-admin persistent delete persistent://codelab-movies/local/job.pending-movies
   ```

2. **Schema** (only when you want to wipe data — re-running *without* this is safe and idempotent):

   ```bash
   docker exec postgres psql -U postgres -d movies -c "DROP SCHEMA public CASCADE; CREATE SCHEMA public;"
   ```

   Restart the megalith once so `ddl-auto: update` recreates the `tmdb_*` tables (plus the demo tables).

3. **Feed** — restart with `--movies.feed=true`. Republishing over an existing subscription is harmless:
   `persist` is idempotent, so duplicates converge.

## Monitor / verify

```bash
# topic status: watch backlog → 0, msgRateRedeliver, unacked
docker exec pulsar bin/pulsar-admin persistent stats persistent://codelab-movies/local/job.pending-movies

# data lives in the 'movies' DATABASE, not 'codelab'
docker exec postgres psql -U postgres -d movies -t -c "SELECT count(*) FROM tmdb_movies;"
docker exec postgres psql -U postgres -d movies -t -c "SELECT relname, n_live_tup FROM pg_stat_user_tables WHERE relname LIKE 'tmdb_%' ORDER BY relname;"

# bad signs in the log: "Import failed for movie", LazyInitialization, DataIntegrityViolation
grep -c "Import failed" <log>
```

A green run shows `backlog=0`, `out = fed count`, `unacked=0`, redeliver rate ~0, and `tmdb_movies` count steady
at `fed − {} markers` (the corpus contains a few files that are literally `{}`, which are skipped, not failed).

## Re-import semantics

`persist`'s re-import path (`apply()`) must treat the two collection kinds differently, or re-imports fail:

- **`@ManyToMany` join sets** (genres, keywords, companies, countries, languages, originCountries) sit on
  composite-PK join tables. They must be **assigned fresh** (`existing.setGenres(...)`) so Hibernate diffs the
  association — `clear()`+`addAll()` re-inserts the same key before the orphan delete flushes →
  `duplicate key value violates unique constraint "tmdb_movie_origin_countries_pkey"`.
- **`orphanRemoval` `@OneToMany` children** (cast, crew, reviews, similar, recommendations) must be **cleared in
  place** (`clear()`+`addAll()`) — whole-reference replacement is rejected by Hibernate ("A collection with orphan
  deletion was no longer referenced by the owning entity instance"). Their rows use generated ids, so re-adding
  never collides.

Consequence: re-imports rewrite the `@OneToMany` children in place (orphan-delete + re-insert), so child identity
ids churn across re-imports while row counts stay flat — by design.

To re-import a specific subset of ids (e.g. movies that failed a previous run), point `--movies.directory` at a
folder containing `<id>.json` files (symlinks work) and feed it with `--movies.feed=true`; only those movies are
touched.

## Post-load indexes

Apply the secondary indexes (`tmdb_schema_finalize.sql`, in this directory) **after** the bulk load and before
heavy reads — index maintenance during a 23M-insert load costs more than it saves. Idempotent; run with bumped
`maintenance_work_mem`:

```bash
{ echo "SET maintenance_work_mem = '1GB';"; cat codelab-movies/tmdb_schema_finalize.sql; } \
  | docker exec -i postgres psql -U postgres -d movies
docker exec postgres psql -U postgres -d movies -c "ANALYZE;"
```

The 29 indexes cover movies read paths (title, imdb id, release date, popularity, vote count, language, status,
collection), reference-name lookups (people/companies/keywords), junction reverse lookups, cast/crew
(movie · person · order/dept), reviews (movie · created), and the soft edges (`similar`/`recs` × related/from).
FKs are created by `ddl-auto` at schema build time; the soft-reference edge columns are indexed but not FK'd
(decision D4 — a referenced movie id may not have a canonical record).

## Schema notes

- The TMDB data lives in the **`movies`** database; the base `spring.datasource.url` (`.../codelab`) is only a
  template that `CodelabDataSourceFactory` rewrites per module.
- `ddl-auto: update` creates and **widens columns in place**. Wider-than-default lengths are baked into the
  entities (`@Column(length=...)`): `tmdb_movies.overview` 2048, `tmdb_movies.homepage` 8192,
  `tmdb_movies.tagline` 1024, `tmdb_movies.title`/`original_title` 512, `tmdb_movie_cast.character` 1024,
  `tmdb_reviews.content` 65536 — the corpus exceeds the JPA default of 255 in these fields, and Postgres rejects
  oversized values outright rather than truncating.