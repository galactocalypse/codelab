package com.codelab.sample.task;

import com.codelab.common.spring.task.CodelabTask;
import com.codelab.common.spring.task.CodelabTaskSpec;
import com.codelab.sample.repository.SampleItemRepository;
import lombok.extern.slf4j.Slf4j;

/**
 * A trivial headless entry point: touches the {@code sample} data source and reports how many rows
 * it holds. It demonstrates running a unit of work against the application context with {@code
 * --codelab.task=sample.ping} without starting a web server.
 */
@Slf4j
@CodelabTaskSpec(
    value = "sample.ping",
    description = "Ping the sample module: count sample_items in the sample database")
public class SamplePingTask implements CodelabTask {

  private final SampleItemRepository repository;

  /** Creates the task with the repository it reads from. */
  public SamplePingTask(SampleItemRepository repository) {
    this.repository = repository;
  }

  @Override
  public int run() {
    long count = repository.count();
    log.info("Sample module ping: {} sample_items present in the 'sample' database", count);
    repository
        .findAll()
        .forEach(item -> log.info("  sample item id={} name={}", item.getId(), item.getName()));
    return 0;
  }
}
