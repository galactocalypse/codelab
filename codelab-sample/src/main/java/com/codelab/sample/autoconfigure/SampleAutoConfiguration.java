package com.codelab.sample.autoconfigure;

import com.codelab.sample.controller.SampleController;
import com.codelab.sample.service.SampleServiceImpl;
import org.apache.pulsar.client.api.DeadLetterPolicy;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/**
 * Wires the beans that the megalith does <em>not</em> discover by scan: REST controllers and
 * {@code @Service} classes. Entities, repositories, Pulsar publishers/subscriptions and {@code
 * CodelabTask}s are registered automatically by {@code CodelabModuleRegistrar} from the module base
 * package.
 */
@AutoConfiguration
@Import({SampleController.class, SampleServiceImpl.class})
public class SampleAutoConfiguration {

  /**
   * Referenced by {@link com.codelab.sample.worker.SampleJobProcessor}'s {@code
   * deadLetterPolicyRef}. Without it a job that always fails would be redelivered forever against
   * {@code ackTimeoutSeconds = 60}.
   */
  @Bean
  public DeadLetterPolicy sampleDeadLetterPolicy() {
    return DeadLetterPolicy.builder().maxRedeliverCount(3).build();
  }
}
