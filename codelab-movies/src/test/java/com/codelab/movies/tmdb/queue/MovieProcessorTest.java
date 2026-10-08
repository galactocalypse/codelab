package com.codelab.movies.tmdb.queue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.codelab.movies.tmdb.model.MovieDetails;
import com.codelab.movies.tmdb.service.TmdbMovieImportService;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * Consumer tests: which messages are acked (data problems — they would fail identically on every
 * redelivery) versus rethrown (persist failures — retried, then parked by the DeadLetterPolicy).
 */
class MovieProcessorTest {

  @TempDir Path moviesDirectory;

  private TmdbMovieImportService importService;
  private MovieProcessor processor;

  @BeforeEach
  void setUp() {
    importService = mock(TmdbMovieImportService.class);
    processor = new MovieProcessor(importService, moviesDirectory.toString());
  }

  @Test
  void parsesTheFileNamedByTheEventAndPersistsIt() throws IOException {
    copyFixture("1704105.json");

    processor.consume(new MovieJob("1704105"));

    ArgumentCaptor<MovieDetails> details = ArgumentCaptor.forClass(MovieDetails.class);
    verify(importService).persist(details.capture());
    assertThat(details.getValue().getId()).isEqualTo(1704105L);
  }

  @Test
  void acksMissingFilesWithoutPersisting() {
    processor.consume(new MovieJob("999999999"));

    verify(importService, never()).persist(any());
  }

  @Test
  void acksUnparseableFilesWithoutPersisting() throws IOException {
    Files.writeString(moviesDirectory.resolve("123.json"), "this is not json");

    processor.consume(new MovieJob("123"));

    verify(importService, never()).persist(any());
  }

  @Test
  void acksEmptyFetchMarkersWithoutPersisting() throws IOException {
    // TMDB writes {} for movies it could not fetch; the id never reaches persist.
    Files.writeString(moviesDirectory.resolve("42.json"), "{}");

    processor.consume(new MovieJob("42"));

    verify(importService, never()).persist(any());
  }

  @Test
  void acksEventsWithoutAnId() {
    processor.consume(new MovieJob());
    processor.consume(null);

    verify(importService, never()).persist(any());
  }

  @Test
  void rethrowsPersistFailuresSoTheMessageIsRedelivered() throws IOException {
    Files.writeString(moviesDirectory.resolve("7.json"), "{\"id\":7,\"title\":\"Seven\"}");
    when(importService.persist(any())).thenThrow(new DataIntegrityViolationException("boom"));

    assertThatThrownBy(() -> processor.consume(new MovieJob("7")))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  private void copyFixture(String fileName) throws IOException {
    try (InputStream in = MovieProcessorTest.class.getResourceAsStream("/" + fileName)) {
      assertThat(in).as("fixture %s on the test classpath", fileName).isNotNull();
      Files.copy(in, moviesDirectory.resolve(fileName));
    }
  }
}
