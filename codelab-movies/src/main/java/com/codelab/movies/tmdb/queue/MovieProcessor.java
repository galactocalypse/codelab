package com.codelab.movies.tmdb.queue;

import com.codelab.common.spring.eventbus.CodelabEventConsumer;
import com.codelab.common.spring.eventbus.CodelabSubscription;
import com.codelab.movies.tmdb.model.MovieDetails;
import com.codelab.movies.tmdb.parser.MovieDetailsLoader;
import com.codelab.movies.tmdb.parser.MovieEvent;
import com.codelab.movies.tmdb.service.TmdbMovieImportService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicLong;
import lombok.extern.slf4j.Slf4j;
import org.apache.pulsar.client.api.SubscriptionInitialPosition;
import org.springframework.beans.factory.annotation.Value;

/**
 * Consumes movie ids from the {@code pending-movies} topic and imports the matching JSON file from
 * {@code ${movies.directory}}.
 *
 * <p>File name == TMDB id, so the event only has to name the file; parsing happens here, one file
 * per message, which is what bounds memory on the consumer side.
 *
 * <p>Data problems (missing file, empty {@code {}} marker, unparseable JSON) are counted and acked
 * — they would fail identically on every redelivery, so retrying them only builds a DLQ of
 * permanent failures. Infrastructure failures ({@code persist} throwing) are rethrown, which
 * negative-acks the message: three redeliveries, then the DeadLetterPolicy parks it on the DLQ
 * topic rather than redelivering forever against {@code ackTimeoutSeconds = 60}.
 */
@Slf4j
@CodelabSubscription(
    topic = "pending-movies",
    subscriptionName = "movies-processor",
    initialPosition = SubscriptionInitialPosition.Earliest,
    concurrency = 4,
    deadLetterPolicyRef = "movieDeadLetterPolicy")
public class MovieProcessor implements CodelabEventConsumer<MovieEvent> {

  private static final long PROGRESS_EVERY = 1_000;

  private final TmdbMovieImportService importService;
  private final Path moviesDirectory;
  private final ObjectMapper objectMapper = new ObjectMapper();

  private final AtomicLong processed = new AtomicLong();
  private final AtomicLong imported = new AtomicLong();
  private final AtomicLong skipped = new AtomicLong();
  private final AtomicLong failed = new AtomicLong();
  private final AtomicLong startedNanos = new AtomicLong();

  public MovieProcessor(
      TmdbMovieImportService importService,
      @Value("${movies.directory:/home/adarsh/code/movie_fetcher/data/movies}")
          String moviesDirectory) {
    this.importService = importService;
    this.moviesDirectory = Path.of(moviesDirectory);
  }

  @Override
  public void consume(MovieEvent event) {
    startedNanos.compareAndSet(0, System.nanoTime());
    try {
      if (event == null || event.getId() == null || event.getId().isBlank()) {
        skipped.incrementAndGet();
        return;
      }

      Path file = moviesDirectory.resolve(event.getId() + ".json");
      if (!Files.isRegularFile(file)) {
        skipped.incrementAndGet();
        log.debug("No movie file {} for id {}", file, event.getId());
        return;
      }

      MovieDetails details;
      try {
        details = MovieDetailsLoader.parseFile(file, objectMapper);
      } catch (Exception parseFailure) {
        skipped.incrementAndGet();
        log.warn("Could not parse {}: {}", file, parseFailure.toString());
        return;
      }

      if (details == null || details.getId() == 0) {
        // TMDB writes {} for movies it could not fetch — a permanent condition, not an error.
        skipped.incrementAndGet();
        return;
      }

      importService.persist(details);
      imported.incrementAndGet();
    } catch (RuntimeException e) {
      failed.incrementAndGet();
      log.warn("Import failed for movie {}", event == null ? null : event.getId(), e);
      throw e;
    } finally {
      progress();
    }
  }

  private void progress() {
    long done = processed.incrementAndGet();
    if (done % PROGRESS_EVERY != 0) {
      return;
    }
    double seconds = (System.nanoTime() - startedNanos.get()) / 1_000_000_000.0;
    double rate = seconds > 0 ? Math.round(done / seconds * 10.0) / 10.0 : 0.0;
    log.info(
        "Movie ingest progress: {} processed ({} imported, {} skipped), {} failed, {} msg/s",
        done,
        imported.get(),
        skipped.get(),
        failed.get(),
        rate);
  }
}
