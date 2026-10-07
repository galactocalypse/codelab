package com.codelab.movies.tmdb.queue;

import com.codelab.movies.tmdb.parser.MovieEvent;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;

/**
 * Publishes one keyed event per movie file in {@code ${movies.directory}} — filenames only, never
 * parses. The file name is the TMDB id, so feeding the full 1.2M-file corpus is a ~60 MB topic of
 * short strings; parsing stays on the consumer side, one file per message.
 *
 * <p>Registered only when {@code movies.feed=true}, so a normal web boot never feeds. Re-running
 * over an existing topic is harmless: {@code TmdbMovieImportService.persist} is idempotent, so
 * duplicate deliveries converge on the same rows.
 */
@Slf4j
public class MovieFeedRunner implements CommandLineRunner {

  private final MovieEventPublisher publisher;
  private final Path moviesDirectory;
  private final long limit;

  /**
   * @param limit maximum number of ids to publish; {@code 0} feeds the whole directory
   */
  public MovieFeedRunner(MovieEventPublisher publisher, Path moviesDirectory, long limit) {
    this.publisher = publisher;
    this.moviesDirectory = moviesDirectory;
    this.limit = limit;
  }

  @Override
  public void run(String... args) throws IOException {
    long published = 0;
    long startNanos = System.nanoTime();
    log.info(
        "Feeding movie ids from {}{}", moviesDirectory, limit > 0 ? " (limit " + limit + ")" : "");

    try (DirectoryStream<Path> stream = Files.newDirectoryStream(moviesDirectory, "*.json")) {
      for (Path file : stream) {
        if (limit > 0 && published >= limit) {
          break;
        }
        String fileName = file.getFileName().toString();
        String id = fileName.substring(0, fileName.length() - ".json".length());
        publisher.publish(id, new MovieEvent(id));
        published++;
        if (published % 10_000 == 0) {
          log.info("...{} movie ids published in {}s", published, elapsedSeconds(startNanos));
        }
      }
    }

    log.info(
        "Feed finished: {} movie ids published in {}s ({} msg/s)",
        published,
        elapsedSeconds(startNanos),
        rate(published, startNanos));
  }

  private static long elapsedSeconds(long startNanos) {
    return (System.nanoTime() - startNanos) / 1_000_000_000;
  }

  private static long rate(long count, long startNanos) {
    double seconds = (System.nanoTime() - startNanos) / 1_000_000_000.0;
    return seconds > 0 ? Math.round(count / seconds) : 0;
  }
}
