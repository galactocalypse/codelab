package com.codelab.megalith.e2e.jobs;

import com.codelab.common.spring.jobbus.CodelabJobConsumer;
import com.codelab.common.spring.jobbus.CodelabJobSubscription;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.pulsar.client.api.SubscriptionInitialPosition;

/**
 * Test job consumer: records the single job the E2E publishes. {@code Earliest} matters because the
 * publish may land before the listener attaches — the assertion is about the job hop, not a race
 * with the broker.
 */
@CodelabJobSubscription(
    topic = "test-work",
    subscriptionName = "e2e-job-observer",
    initialPosition = SubscriptionInitialPosition.Earliest,
    concurrency = 1)
public class RecordingJobConsumer implements CodelabJobConsumer<JobBusProbe> {

  private final CountDownLatch received = new CountDownLatch(1);
  private final AtomicReference<JobBusProbe> job = new AtomicReference<>();

  @Override
  public void consume(JobBusProbe payload) {
    job.set(payload);
    received.countDown();
  }

  /** Blocks until the job arrives, or the timeout elapses. */
  public boolean awaitJob(long timeout, TimeUnit unit) throws InterruptedException {
    return received.await(timeout, unit);
  }

  /** The last job seen, or {@code null} if none arrived. */
  public JobBusProbe job() {
    return job.get();
  }
}
