package com.codelab.megalith;

import com.codelab.movies.tmdb.model.MovieDetails;
import com.codelab.movies.tmdb.parser.MovieDetailsLoader;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Entry point for running the movie-import job as a Spring Boot application. This is a batch job,
 * not a web service - it has no web starter on the classpath, so SpringApplication won't start an
 * embedded server; it just builds the ApplicationContext, runs every CommandLineRunner bean, then
 * SpringApplication.exit(...) below tears the context down and returns an exit code so the JVM
 * doesn't hang around afterwards.
 */
@SpringBootApplication(
    exclude = {DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class})
public class MovieFetcherApplication {

  static void main(String[] args) {
    ConfigurableApplicationContext context =
        SpringApplication.run(MovieFetcherApplication.class, args);
    int exitCode = SpringApplication.exit(context);
    System.exit(exitCode);
  }

  // Spring Boot's own Jackson auto-configuration needs Jackson2ObjectMapperBuilder,
  // which lives in spring-web - not present here since this isn't a web app.
  // Defining the bean ourselves avoids pulling in spring-web just for this.
  @Bean
  public ObjectMapper objectMapper() {
    return new ObjectMapper();
  }

  // @Profile, not just @Bean, because MegalithApplication's component scan picks up
  // MovieFetcherApplication itself (same package, and @SpringBootApplication is a @Component):
  // any bean method here would also be registered in the web app. Run this job explicitly with
  // --spring.profiles.active=importer.
  @Component
  @Profile("importer")
  static class MovieImportRunner implements CommandLineRunner {

    private final ObjectMapper objectMapper;

    // Override with --movies.directory=/some/other/path, or set it in
    // application.properties / application.yml.
    @Value("${movies.directory:/home/adarsh/code/movie_fetcher/data/movies}")
    private String moviesDirectory;

    MovieImportRunner(ObjectMapper objectMapper) {
      this.objectMapper = objectMapper;
    }

    @Override
    public void run(String... args) throws Exception {
      Path dir = Paths.get(moviesDirectory);

      Map<String, AtomicLong> movieCountByGenre = new ConcurrentHashMap<>();

      MovieDetailsLoader.Result result =
          MovieDetailsLoader.processDirectory(
              dir,
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
    }
  }
}
