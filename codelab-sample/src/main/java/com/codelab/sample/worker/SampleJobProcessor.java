package com.codelab.sample.worker;

import com.codelab.common.spring.jobbus.CodelabJobConsumer;
import com.codelab.common.spring.jobbus.CodelabJobSubscription;
import com.codelab.sample.model.SampleJob;
import com.codelab.sample.repository.SampleItemRepository;
import lombok.extern.slf4j.Slf4j;

/**
 * Example job consumer. Processes one {@link SampleJob}: loads the referenced item inside the
 * module's own transaction and logs it. A thrown exception negative-acks the message; after {@code
 * maxRedeliverCount=3} the {@code sampleDeadLetterPolicy} parks it on the DLQ.
 *
 * <p>Transactions must qualify the transaction manager explicitly - none is {@code @Primary} in the
 * megalith. The Spring-transactional alias below keeps the import readable.
 */
@Slf4j
@CodelabJobSubscription(
    topic = "pending-samples",
    subscriptionName = "samples-processor",
    initialPosition = org.apache.pulsar.client.api.SubscriptionInitialPosition.Earliest,
    concurrency = 2,
    deadLetterPolicyRef = "sampleDeadLetterPolicy")
public class SampleJobProcessor implements CodelabJobConsumer<SampleJob> {

  private final SampleItemRepository repository;

  /** Creates the processor with the repository it reads. */
  public SampleJobProcessor(SampleItemRepository repository) {
    this.repository = repository;
  }

  @Override
  @org.springframework.transaction.annotation.Transactional(
      transactionManager = "sample.transactionManager")
  public void consume(SampleJob job) {
    if (job == null || job.getSampleId() == null) {
      log.warn("Ignoring job with no sampleId");
      return;
    }
    repository
        .findById(job.getSampleId())
        .ifPresentOrElse(
            item -> log.info("Processed sample job: id={} name={}", item.getId(), item.getName()),
            () -> log.warn("Sample item {} not found while processing job", job.getSampleId()));
  }
}
