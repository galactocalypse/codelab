package com.codelab.movies.tmdb.queue;

import static org.assertj.core.api.Assertions.assertThat;

import com.codelab.common.spring.eventbus.BusinessVersion;
import com.codelab.movies.tmdb.parser.MovieEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Feeder tests: which files are published, and where the {@code movies.feed.limit} gate stops. */
class MovieFeedRunnerTest {

  @TempDir Path moviesDirectory;

  @Test
  void publishesEveryJsonFileKeyedByItsId() throws IOException {
    write("1.json");
    write("2.json");
    write("3.json");
    Files.writeString(moviesDirectory.resolve("notes.txt"), "not a movie");

    RecordingPublisher publisher = new RecordingPublisher();
    new MovieFeedRunner(publisher, moviesDirectory, 0).run();

    assertThat(publisher.events).containsOnlyKeys("1", "2", "3");
    assertThat(publisher.events.get("1").getId()).isEqualTo("1");
  }

  @Test
  void stopsAtTheConfiguredLimit() throws IOException {
    write("1.json");
    write("2.json");
    write("3.json");

    RecordingPublisher publisher = new RecordingPublisher();
    new MovieFeedRunner(publisher, moviesDirectory, 2).run();

    assertThat(publisher.events).hasSize(2);
  }

  @Test
  void publishesNothingForAnEmptyDirectory() throws IOException {
    RecordingPublisher publisher = new RecordingPublisher();

    new MovieFeedRunner(publisher, moviesDirectory, 0).run();

    assertThat(publisher.events).isEmpty();
  }

  private void write(String fileName) throws IOException {
    Files.writeString(moviesDirectory.resolve(fileName), "{}");
  }

  private static final class RecordingPublisher implements MovieEventPublisher {

    private final Map<String, MovieEvent> events = new LinkedHashMap<>();

    @Override
    public void publish(BusinessVersion version, MovieEvent event) {
      publish(null, version, event);
    }

    @Override
    public void publish(String key, BusinessVersion version, MovieEvent event) {
      events.put(key, event);
    }
  }
}
