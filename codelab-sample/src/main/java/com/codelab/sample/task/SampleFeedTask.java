package com.codelab.sample.task;

import com.codelab.common.spring.task.CodelabTask;
import com.codelab.common.spring.task.CodelabTaskSpec;
import com.codelab.sample.model.SampleJob;
import com.codelab.sample.publisher.SampleJobPublisher;
import com.codelab.sample.repository.SampleItemRepository;
import lombok.extern.slf4j.Slf4j;

/**
 * Publishes one {@link SampleJob} per persisted sample item. Demonstrates job-queue production from
 * a headless task, mirroring {@code movies.feed} but reading from the {@code sample} database.
 */
@Slf4j
@CodelabTaskSpec(
    value = "sample.feed",
    description = "Publish a SampleJob for every row in sample_items")
public class SampleFeedTask implements CodelabTask {

  private final SampleJobPublisher publisher;
  private final SampleItemRepository repository;

  /** Creates the task with the publisher it feeds and the repository it reads. */
  public SampleFeedTask(SampleJobPublisher publisher, SampleItemRepository repository) {
    this.publisher = publisher;
    this.repository = repository;
  }

  @Override
  public int run() {
    long published = 0;
    for (var item : repository.findAll()) {
      publisher.publish(new SampleJob(item.getId()));
      published++;
    }
    log.info("Fed {} sample jobs onto job.pending-samples", published);
    return 0;
  }
}
