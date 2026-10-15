package com.codelab.movies.autoconfigure;

import com.codelab.movies.tmdb.service.TmdbMovieImportService;
import com.codelab.movies.tmdb.service.TmdbMovieMapper;
import org.apache.pulsar.client.api.DeadLetterPolicy;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@Import({TmdbMovieMapper.class, TmdbMovieImportService.class})
public class MoviesAutoConfiguration {

  /**
   * Referenced by {@code MovieProcessor}'s {@code deadLetterPolicyRef}. Without it, a persist that
   * always fails would be redelivered forever against {@code ackTimeoutSeconds = 60}.
   */
  @Bean
  public DeadLetterPolicy movieDeadLetterPolicy() {
    return DeadLetterPolicy.builder().maxRedeliverCount(3).build();
  }
}
