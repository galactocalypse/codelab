package com.codelab.movies.autoconfigure;

import com.codelab.movies.*;
import com.codelab.movies.service.MovieCreditService;
import com.codelab.movies.service.MovieRoleService;
import com.codelab.movies.service.MovieService;
import com.codelab.movies.service.PersonService;
import com.codelab.movies.tmdb.queue.MovieEventPublisher;
import com.codelab.movies.tmdb.queue.MovieFeedRunner;
import com.codelab.movies.tmdb.service.TmdbMovieImportService;
import com.codelab.movies.tmdb.service.TmdbMovieMapper;
import java.nio.file.Path;
import org.apache.pulsar.client.api.DeadLetterPolicy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@Import({
  MovieController.class,
  MovieCreditController.class,
  MovieRoleController.class,
  PersonController.class,
  MovieService.class,
  MovieCreditService.class,
  MovieRoleService.class,
  PersonService.class,
  TmdbMovieMapper.class,
  TmdbMovieImportService.class
})
public class MoviesAutoConfiguration {

  /**
   * Referenced by {@code MovieProcessor}'s {@code deadLetterPolicyRef}. Without it, a persist that
   * always fails would be redelivered forever against {@code ackTimeoutSeconds = 60}.
   */
  @Bean
  public DeadLetterPolicy movieDeadLetterPolicy() {
    return DeadLetterPolicy.builder().maxRedeliverCount(3).build();
  }

  /** Feeds ids onto the pending-movies topic; only active when {@code --movies.feed=true}. */
  @Bean
  @ConditionalOnProperty(name = "movies.feed", havingValue = "true")
  public MovieFeedRunner movieFeedRunner(
      MovieEventPublisher publisher,
      @Value("${movies.directory:/home/adarsh/code/movie_fetcher/data/movies}")
          String moviesDirectory,
      @Value("${movies.feed.limit:0}") long limit) {
    return new MovieFeedRunner(publisher, Path.of(moviesDirectory), limit);
  }
}
