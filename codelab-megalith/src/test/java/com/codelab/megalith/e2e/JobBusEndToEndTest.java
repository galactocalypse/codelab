package com.codelab.megalith.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import com.codelab.megalith.e2e.jobs.JobBusProbe;
import com.codelab.megalith.e2e.jobs.RecordingJobConsumer;
import com.codelab.megalith.e2e.jobs.TestJobPublisher;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.pulsar.PulsarContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * End-to-end test of the job bus over a real Pulsar broker, driven the way the application drives
 * it: a manual publish through the real {@link TestJobPublisher} proxy (size guard, keying, no
 * business version) received by a real {@code CodelabJobConsumer} listener registered by the
 * framework scanner.
 *
 * <p>This is the job-path counterpart to {@link CdcJobEventEndToEndTest}, which is event-oriented:
 * jobs are never produced by CDC, so the job path is only ever exercised by an application-level
 * publish like this one.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(
    classes = CdcEndToEndApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    properties = {"app.pulsar.tenant.prefix=codelab-", "app.pulsar.namespace=local"})
class JobBusEndToEndTest {

  @Container
  static final PulsarContainer PULSAR =
      new PulsarContainer(DockerImageName.parse("apachepulsar/pulsar:4.2.4"));

  @DynamicPropertySource
  static void pulsarProperties(DynamicPropertyRegistry registry) throws Exception {
    registry.add("spring.pulsar.client.service-url", PULSAR::getPulsarBrokerUrl);
    registry.add("spring.pulsar.admin.service-url", PULSAR::getHttpServiceUrl);
    PulsarTestSupport.provisionModuleTenant(PULSAR);
  }

  @Autowired TestJobPublisher publisher;
  @Autowired RecordingJobConsumer consumer;

  @Test
  void manualPublishIsConsumedOverTheBroker() throws Exception {
    publisher.publish("job-42", JobBusProbe.builder().value("hello").build());

    assertThat(consumer.awaitJob(30, TimeUnit.SECONDS))
        .as("job consumed from the job bus within the timeout")
        .isTrue();
    assertThat(consumer.job()).isNotNull();
    assertThat(consumer.job().getValue()).isEqualTo("hello");
  }
}
