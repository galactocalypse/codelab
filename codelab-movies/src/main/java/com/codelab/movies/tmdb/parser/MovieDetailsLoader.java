package com.codelab.movies.tmdb.parser;

import com.codelab.movies.tmdb.model.MovieDetails;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * Streams every "*.json" file in a directory into a {@link MovieDetails} object. Built for
 * directories with hundreds of thousands of files: - never lists the whole directory into memory
 * (uses a lazy DirectoryStream) - never holds more than one parsed movie per worker thread at a
 * time - a single corrupt/unreadable file is logged and skipped, not fatal
 */
public final class MovieDetailsLoader {

  private MovieDetailsLoader() {}

  /** Parse a single movie JSON file. */
  public static MovieDetails parseFile(Path file, ObjectMapper mapper) throws IOException {
    return mapper.readValue(file.toFile(), MovieDetails.class);
  }

  /**
   * Parses every "*.json" file directly inside {@code dir} and hands each result to {@code
   * consumer} as soon as it's ready. Work is spread across all available cores, so {@code consumer}
   * will be called concurrently from multiple threads - make sure whatever it does (accumulate
   * stats, write to a DB, build an index...) is thread-safe.
   *
   * @return counts of how many files parsed cleanly vs. failed, and elapsed time
   */
  /** Convenience overload for standalone (non-Spring) use - builds its own default ObjectMapper. */
  public static Result processDirectory(Path dir, Consumer<MovieDetails> consumer)
      throws IOException {
    return processDirectory(dir, consumer, new ObjectMapper());
  }

  /**
   * Same as {@link #processDirectory(Path, Consumer)}, but takes the ObjectMapper to use - pass in
   * a Spring-managed bean so it picks up whatever Jackson config (modules, naming strategy, etc.)
   * your application already defines.
   */
  public static Result processDirectory(
      Path dir, Consumer<MovieDetails> consumer, ObjectMapper mapper) throws IOException {
    AtomicLong succeeded = new AtomicLong();
    AtomicLong failed = new AtomicLong();
    long start = System.nanoTime();

    try (ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor();
        DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.json")) {
      for (Path file : stream) {
        pool.submit(
            () -> {
              try {
                MovieDetails movie = parseFile(file, mapper);
                consumer.accept(movie);
                long done = succeeded.incrementAndGet();
                if (done % 10_000 == 0) {
                  System.out.printf(
                      "...%,d files parsed in %ds %n",
                      done, (System.nanoTime() - start) / 1000_000_000);
                }
              } catch (Exception e) {
                failed.incrementAndGet();
                System.err.println("Failed to parse " + file + ": " + e.getMessage());
              }
            });
      }
    }

    long elapsedMs = (System.nanoTime() - start) / 1_000_000;
    return new Result(succeeded.get(), failed.get(), elapsedMs);
  }

  public static final class Result {
    public final long succeeded;
    public final long failed;
    public final long elapsedMs;

    Result(long succeeded, long failed, long elapsedMs) {
      this.succeeded = succeeded;
      this.failed = failed;
      this.elapsedMs = elapsedMs;
    }

    public void print() {
      System.out.printf("Done: %,d parsed, %,d failed, in %,d ms%n", succeeded, failed, elapsedMs);
    }
  }
}
