package com.codelab.sample.worker;

import com.codelab.common.spring.eventbus.BusinessVersion;
import com.codelab.common.spring.eventbus.CodelabEventConsumer;
import com.codelab.common.spring.eventbus.CodelabSubscription;
import com.codelab.sample.model.SampleCreatedEvent;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

/**
 * Example event consumer. Declaring {@code created-samples} + {@code created-samples-logger} gives
 * an independent consumer cursor on that topic; a second consumer with the same pair would share
 * it. Unsupported business versions are routed straight to the DLQ by the framework before {@link
 * #consume(SampleCreatedEvent)} is ever called.
 */
@Slf4j
@CodelabSubscription(
    topic = "created-samples",
    subscriptionName = "created-samples-logger",
    concurrency = 1)
public class SampleCreatedEventProcessor implements CodelabEventConsumer<SampleCreatedEvent> {

  @Override
  public Set<BusinessVersion> getSupportedBusinessVersions() {
    return Set.of(BusinessVersion.of("v1"), BusinessVersion.LEGACY);
  }

  @Override
  public void consume(SampleCreatedEvent event) {
    log.info("Sample item created: sampleId={}", event.getSampleId());
  }
}
