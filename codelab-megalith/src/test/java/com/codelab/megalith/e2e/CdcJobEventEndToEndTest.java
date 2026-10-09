package com.codelab.megalith.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import com.codelab.orders.model.OrderCreatedEvent;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import org.apache.pulsar.client.api.Producer;
import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.Schema;
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
 * End-to-end test of the CDC path over a real Pulsar broker. It is <em>event-oriented</em>: the CDC
 * envelope is treated as plumbing and the assertion is on the business event it produces.
 *
 * <pre>
 *   Debezium-shaped envelope on job.orders-cdc.public.orders
 *     -> real job consumer  (OrderCdcNormalizer, registered by the real scanner)
 *       -> versioned event  (published through the real event-publisher proxy)
 *         -> real event consumer (CodelabVersionAwareMessageDispatcher gate + observer)
 * </pre>
 *
 * <p>What a unit test on the normalizer can't prove is covered here: the kind-prefixed topic names
 * the framework resolves at both ends, the real JSON producer/consumer schemas, the registered
 * subscription topology, and the version gate accepting a live message. Debezium itself is not in
 * the loop — the envelope it would produce is seeded directly, so this stays fast and
 * deterministic. The application-driven job bus is verified separately in {@link
 * JobBusEndToEndTest}, since jobs are never produced by CDC.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(
    classes = CdcEndToEndApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    properties = {"app.pulsar.tenant.prefix=codelab-", "app.pulsar.namespace=local"})
class CdcJobEventEndToEndTest {

  private static final String CDC_TOPIC =
      "persistent://codelab-orders/local/job.orders-cdc.public.orders";

  /** Realistic Debezium envelope; source.name carries the "job." prefix from topic.prefix. */
  private static final String DEBEZIUM_INSERT_ENVELOPE =
      """
      {
        "before": null,
        "after": {"id": 42, "status": "CREATED", "created_at_date": "2026-10-08"},
        "op": "c",
        "source": {
          "version": "3.6.3.Final",
          "connector": "postgresql",
          "name": "job.orders-cdc",
          "ts_ms": 1759937000000,
          "db": "orders",
          "schema": "public",
          "table": "orders"
        },
        "ts_ms": 1759937000123
      }
      """;

  @Container
  static final PulsarContainer PULSAR =
      new PulsarContainer(DockerImageName.parse("apachepulsar/pulsar:4.2.4"));

  @DynamicPropertySource
  static void pulsarProperties(DynamicPropertyRegistry registry) throws Exception {
    registry.add("spring.pulsar.client.service-url", PULSAR::getPulsarBrokerUrl);
    registry.add("spring.pulsar.admin.service-url", PULSAR::getHttpServiceUrl);
    PulsarTestSupport.provisionModuleTenant(PULSAR);
  }

  @Autowired PulsarClient pulsarClient;
  @Autowired RecordingOrderCreatedEventConsumer observer;

  @Test
  void cdcInsertBecomesVersionedEventOnTheEventBus() throws Exception {
    try (Producer<byte[]> producer =
        pulsarClient.newProducer(Schema.BYTES).topic(CDC_TOPIC).create()) {
      producer.send(DEBEZIUM_INSERT_ENVELOPE.getBytes(StandardCharsets.UTF_8));
    }

    assertThat(observer.awaitEvent(30, TimeUnit.SECONDS))
        .as("OrderCreatedEvent consumed from the event bus within the timeout")
        .isTrue();

    OrderCreatedEvent event = observer.event();
    assertThat(event).isNotNull();
    assertThat(event.getOrderId()).isEqualTo(42L);
  }
}
