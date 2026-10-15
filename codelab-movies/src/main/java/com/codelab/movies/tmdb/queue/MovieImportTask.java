package com.codelab.movies.tmdb.queue;

import com.codelab.common.spring.task.CodelabTask;
import com.codelab.common.spring.task.CodelabTaskSpec;
import com.codelab.movies.tmdb.model.MovieDetails;
import com.codelab.movies.tmdb.parser.MovieDetailsLoader;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.beans.factory.annotation.Value;

/**
 * Walks {@code ${movies.directory}}, parses every movie file in-process and reports a genre
 * histogram.
 *
 * <p>Unlike {@link MovieFeedTask}, which only publishes ids for the consumer side to parse, this
 * parses on the application side and touches no persistence — a reporting/diagnostic task. It runs
 * only when selected with {@code --codelab.task=movies.import}.
 */
@CodelabTaskSpec(
    value = "movies.import",
    description = "Parse movies.directory in-process and print a genre histogram")
public class MovieImportTask implements CodelabTask {

  private final ObjectMapper objectMapper;
  private final Path moviesDirectory;

  public MovieImportTask(
      ObjectMapper objectMapper,
      @Value("${movies.directory:/home/adarsh/code/movie_fetcher/data/movies}")
          String moviesDirectory) {
    this.objectMapper = objectMapper;
    this.moviesDirectory = Path.of(moviesDirectory);
  }

  @Override
  public int run() throws Exception {
    Map<String, AtomicLong> movieCountByGenre = new ConcurrentHashMap<>();

    MovieDetailsLoader.Result result =
        MovieDetailsLoader.processDirectory(
            moviesDirectory,
            (MovieDetails movie) -> {
              if (movie.getGenres() != null) {
                movie
                    .getGenres()
                    .forEach(
                        g ->
                            movieCountByGenre
                                .computeIfAbsent(g.getName(), k -> new AtomicLong())
                                .incrementAndGet());
              }
            },
            objectMapper);

    result.print();
    movieCountByGenre.forEach(
        (genre, count) -> System.out.printf("%-15s %,d%n", genre, count.get()));
    return 0;
  }
}
