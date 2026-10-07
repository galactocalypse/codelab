-- ============================================================================
-- tmdb_schema_finalize.sql — post-load secondary indexes for the TMDB tables
-- Mirrors movie_fetcher/schema_finalize.sql (the proven Python loader's runbook),
-- adapted to the tmdb_* namespace and to the Hibernate-created schema.
-- Apply AFTER the bulk load. Idempotent (CREATE INDEX IF NOT EXISTS).
-- FKs already exist (created by ddl-auto), so none are added here. The soft
-- reference columns (similar_movie_id / recommended_movie_id) are intentionally
-- indexed but NOT foreign keys, per decision D4.
-- ============================================================================

-- ----------------------------------------------------------------------------
-- tmdb_movies
-- ----------------------------------------------------------------------------

CREATE INDEX IF NOT EXISTS idx_tmdb_movies_imdb_id        ON tmdb_movies (imdb_id);
CREATE INDEX IF NOT EXISTS idx_tmdb_movies_release_date   ON tmdb_movies (release_date);
CREATE INDEX IF NOT EXISTS idx_tmdb_movies_popularity     ON tmdb_movies (popularity DESC);
CREATE INDEX IF NOT EXISTS idx_tmdb_movies_vote_count     ON tmdb_movies (vote_count DESC);
CREATE INDEX IF NOT EXISTS idx_tmdb_movies_language       ON tmdb_movies (original_language);
CREATE INDEX IF NOT EXISTS idx_tmdb_movies_status         ON tmdb_movies (status);
CREATE INDEX IF NOT EXISTS idx_tmdb_movies_collection     ON tmdb_movies (collection_id);
CREATE INDEX IF NOT EXISTS idx_tmdb_movies_title          ON tmdb_movies (title);

-- ----------------------------------------------------------------------------
-- Reference tables
-- ----------------------------------------------------------------------------

CREATE INDEX IF NOT EXISTS idx_tmdb_keywords_name    ON tmdb_keywords (name);
CREATE INDEX IF NOT EXISTS idx_tmdb_companies_name   ON tmdb_companies (name);
CREATE INDEX IF NOT EXISTS idx_tmdb_people_name      ON tmdb_people (name);

-- ----------------------------------------------------------------------------
-- Junctions: reverse lookup indexes
-- ----------------------------------------------------------------------------

CREATE INDEX IF NOT EXISTS idx_tmdb_movie_genres_ref    ON tmdb_movie_genres (genre_id, movie_id);
CREATE INDEX IF NOT EXISTS idx_tmdb_movie_keywords_ref  ON tmdb_movie_keywords (keyword_id, movie_id);
CREATE INDEX IF NOT EXISTS idx_tmdb_movie_companies_ref ON tmdb_movie_companies (company_id, movie_id);
CREATE INDEX IF NOT EXISTS idx_tmdb_movie_countries_ref ON tmdb_movie_countries (iso_3166_1, movie_id);
CREATE INDEX IF NOT EXISTS idx_tmdb_movie_languages_ref ON tmdb_movie_languages (iso_639_1, movie_id);
CREATE INDEX IF NOT EXISTS idx_tmdb_movie_origin_ref    ON tmdb_movie_origin_countries (iso_3166_1, movie_id);

-- ----------------------------------------------------------------------------
-- Credits
-- ----------------------------------------------------------------------------

CREATE INDEX IF NOT EXISTS idx_tmdb_cast_movie   ON tmdb_movie_cast (movie_id);
CREATE INDEX IF NOT EXISTS idx_tmdb_cast_person  ON tmdb_movie_cast (person_id);
CREATE INDEX IF NOT EXISTS idx_tmdb_cast_order   ON tmdb_movie_cast (movie_id, cast_order);

CREATE INDEX IF NOT EXISTS idx_tmdb_crew_movie   ON tmdb_movie_crew (movie_id);
CREATE INDEX IF NOT EXISTS idx_tmdb_crew_person  ON tmdb_movie_crew (person_id);
CREATE INDEX IF NOT EXISTS idx_tmdb_crew_dept    ON tmdb_movie_crew (movie_id, department);

-- ----------------------------------------------------------------------------
-- Reviews
-- ----------------------------------------------------------------------------

CREATE INDEX IF NOT EXISTS idx_tmdb_reviews_movie    ON tmdb_reviews (movie_id);
CREATE INDEX IF NOT EXISTS idx_tmdb_reviews_created  ON tmdb_reviews (created_at);

-- ----------------------------------------------------------------------------
-- Edges (soft references)
-- ----------------------------------------------------------------------------

CREATE INDEX IF NOT EXISTS idx_tmdb_similar_related ON tmdb_movie_similar (similar_movie_id);
CREATE INDEX IF NOT EXISTS idx_tmdb_similar_from    ON tmdb_movie_similar (movie_id, list_order);
CREATE INDEX IF NOT EXISTS idx_tmdb_recs_related    ON tmdb_movie_recommendations (recommended_movie_id);
CREATE INDEX IF NOT EXISTS idx_tmdb_recs_from       ON tmdb_movie_recommendations (movie_id, list_order);