package com.codelab.movies.tmdb.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.codelab.movies.tmdb.model.MovieDetails;
import com.codelab.movies.tmdb.repository.TmdbGenreRepository;
import com.codelab.movies.tmdb.repository.TmdbKeywordRepository;
import com.codelab.movies.tmdb.repository.TmdbMovieCastRepository;
import com.codelab.movies.tmdb.repository.TmdbMovieCrewRepository;
import com.codelab.movies.tmdb.repository.TmdbMovieRepository;
import com.codelab.movies.tmdb.repository.TmdbPersonRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Integration test for the import path against real Postgres.
 *
 * <p>Two things are checked here that only a database can check: that the schema Hibernate
 * generates from the entities is valid Postgres, and that importing the same file twice converges
 * on the same rows instead of duplicating them. The idempotency guarantee is what makes a resumable
 * 1.2M-file run possible, and it depends entirely on {@code orphanRemoval} actually firing on
 * re-import.
 *
 * <p>H2 would not do: {@code ddl-auto} on H2 accepts DDL Postgres rejects, which would let the test
 * pass while production fails.
 */
@Testcontainers(disabledWithoutDocker = true)
@DataJpaTest(showSql = false, properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class TmdbMovieImportServiceTest {

  @Container
  static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>("postgres:18").withDatabaseName("movies");

  /**
   * The service resolves its transaction manager by the name the module registrar gives it in
   * production. Nothing in this slice knows that name, so it is aliased onto the single manager
   * Boot configures — one bean, both names, rather than a second manager competing for the same
   * {@code EntityManagerFactory}.
   */
  @TestConfiguration
  @Import({TmdbMovieMapper.class, TmdbMovieImportService.class})
  static class Wiring {

    @Bean
    static BeanFactoryPostProcessor aliasMoviesTransactionManager() {
      return beanFactory ->
          beanFactory.registerAlias("transactionManager", "movies.transactionManager");
    }
  }

  @DynamicPropertySource
  static void postgresProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
  }

  private final TmdbMovieImportService service;
  private final TmdbMovieRepository movies;
  private final TmdbMovieCastRepository cast;
  private final TmdbMovieCrewRepository crew;
  private final TmdbGenreRepository genres;
  private final TmdbKeywordRepository keywords;
  private final TmdbPersonRepository people;

  @Autowired
  TmdbMovieImportServiceTest(
      TmdbMovieImportService service,
      TmdbMovieRepository movies,
      TmdbMovieCastRepository cast,
      TmdbMovieCrewRepository crew,
      TmdbGenreRepository genres,
      TmdbKeywordRepository keywords,
      TmdbPersonRepository people) {
    this.service = service;
    this.movies = movies;
    this.cast = cast;
    this.crew = crew;
    this.genres = genres;
    this.keywords = keywords;
    this.people = people;
  }

  @Test
  void importingTheSameMovieTwiceConvergesOnTheSameRows() throws IOException {
    MovieDetails details = sample();

    assertThat(service.persist(details)).isTrue();
    // A second run must update the row it already wrote rather than append a copy of it.
    details.setTitle("Limiar (revised)");
    assertThat(service.persist(details)).isTrue();

    assertThat(movies.count()).isEqualTo(1);
    assertThat(cast.count()).isEqualTo(3);
    assertThat(crew.count()).isEqualTo(11);
    assertThat(genres.count()).isEqualTo(2);
    assertThat(keywords.count()).isEqualTo(2);

    // The single row carries the second run's values, and its children were replaced rather than
    // appended to — the orphanRemoval the whole design leans on.
    assertThat(movies.findById(1704105L))
        .hasValueSatisfying(
            movie -> {
              assertThat(movie.getTitle()).isEqualTo("Limiar (revised)");
              assertThat(movie.getCast()).hasSize(3);
              assertThat(movie.getCrew()).hasSize(11);
              assertThat(movie.getGenres()).hasSize(2);
              assertThat(movie.getKeywords()).hasSize(2);
            });

    // Cast and crew de-duplicate onto one person row per TMDB id.
    assertThat(people.count()).isEqualTo(12);
  }

  @Test
  void emptyFetchMarkersAreSkipped() throws IOException {
    MovieDetails empty = new ObjectMapper().readValue("{}", MovieDetails.class);

    assertThat(service.persist(empty)).isFalse();
    assertThat(service.persist(null)).isFalse();
    assertThat(movies.count()).isZero();
  }

  private static MovieDetails sample() throws IOException {
    try (InputStream json = TmdbMovieImportServiceTest.class.getResourceAsStream("/1704105.json")) {
      return new ObjectMapper().readValue(json, MovieDetails.class);
    }
  }
}
