package com.codelab.orders.cdc;

import static org.assertj.core.api.Assertions.assertThat;

import com.codelab.common.spring.eventbus.BusinessVersion;
import com.codelab.orders.model.OrderCreatedEvent;
import com.codelab.orders.model.OrderUpdatedEvent;
import com.codelab.orders.publisher.OrderCreatedEventPublisher;
import com.codelab.orders.publisher.OrderUpdatedEventPublisher;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.apache.pulsar.client.api.Schema;
import org.junit.jupiter.api.Test;

class OrderCdcNormalizerTest {

  /** Realistic Debezium envelope: source block and extra row columns the normalizer ignores. */
  private static final String INSERT_ENVELOPE =
      """
      {
        "before": null,
        "after": {"id": 42, "status": "CREATED", "created_at_date": "2026-10-08"},
        "op": "c",
        "source": {
          "version": "3.6.3.Final",
          "connector": "postgresql",
          "name": "orders-cdc",
          "ts_ms": 1759937000000,
          "db": "orders",
          "schema": "public",
          "table": "orders"
        },
        "ts_ms": 1759937000123
      }
      """;

  private final RecordingCreatedPublisher created = new RecordingCreatedPublisher();
  private final RecordingUpdatedPublisher updated = new RecordingUpdatedPublisher();
  private final OrderCdcNormalizer normalizer = new OrderCdcNormalizer(created, updated);

  @Test
  void decodesPulsarJsonEnvelopeIgnoringUnknownFields() {
    CdcOrderChange decoded =
        Schema.JSON(CdcOrderChange.class).decode(INSERT_ENVELOPE.getBytes(StandardCharsets.UTF_8));

    assertThat(decoded.getOp()).isEqualTo("c");
    assertThat(decoded.getAfter().getId()).isEqualTo(42L);
    assertThat(decoded.getBefore()).isNull();
  }

  @Test
  void insertBecomesVersionedOrderCreatedEventKeyedByOrderId() {
    normalizer.consume(decode(INSERT_ENVELOPE));

    assertThat(created.events).hasSize(1);
    assertThat(created.keys).containsExactly("42");
    assertThat(created.versions).containsExactly(BusinessVersion.of("v1"));
    assertThat(created.events.getFirst().getOrderId()).isEqualTo(42L);
    assertThat(updated.events).isEmpty();
  }

  @Test
  void updateBecomesVersionedOrderUpdatedEventWithoutKey() {
    String updateEnvelope =
        INSERT_ENVELOPE
            .replace("\"op\": \"c\"", "\"op\": \"u\"")
            .replace("\"status\": \"CREATED\"", "\"status\": \"SHIPPED\"");

    normalizer.consume(decode(updateEnvelope));

    assertThat(updated.events).hasSize(1);
    assertThat(updated.versions).containsExactly(BusinessVersion.of("v1"));
    assertThat(updated.events.getFirst().getOrderId()).isEqualTo(42L);
    assertThat(created.events).isEmpty();
  }

  @Test
  void deleteAndSnapshotProduceNoEvent() {
    normalizer.consume(decode(INSERT_ENVELOPE.replace("\"op\": \"c\"", "\"op\": \"d\"")));
    normalizer.consume(decode(INSERT_ENVELOPE.replace("\"op\": \"c\"", "\"op\": \"r\"")));

    assertThat(created.events).isEmpty();
    assertThat(updated.events).isEmpty();
  }

  @Test
  void nullChangeNullOpOrMissingRowAreIgnored() {
    normalizer.consume(null);
    normalizer.consume(new CdcOrderChange());
    normalizer.consume(CdcOrderChange.builder().op("c").build());
    normalizer.consume(
        CdcOrderChange.builder().op("c").after(CdcOrderRow.builder().build()).build());

    assertThat(created.events).isEmpty();
    assertThat(updated.events).isEmpty();
  }

  private static CdcOrderChange decode(String envelope) {
    return Schema.JSON(CdcOrderChange.class).decode(envelope.getBytes(StandardCharsets.UTF_8));
  }

  private static final class RecordingCreatedPublisher implements OrderCreatedEventPublisher {
    private final List<OrderCreatedEvent> events = new ArrayList<>();
    private final List<String> keys = new ArrayList<>();
    private final List<BusinessVersion> versions = new ArrayList<>();

    @Override
    public void publish(String key, BusinessVersion version, OrderCreatedEvent event) {
      keys.add(key);
      versions.add(version);
      events.add(event);
    }

    @Override
    public void publish(BusinessVersion version, OrderCreatedEvent event) {
      publish(null, version, event);
    }
  }

  private static final class RecordingUpdatedPublisher implements OrderUpdatedEventPublisher {
    private final List<OrderUpdatedEvent> events = new ArrayList<>();
    private final List<BusinessVersion> versions = new ArrayList<>();

    @Override
    public void publish(BusinessVersion version, OrderUpdatedEvent event) {
      versions.add(version);
      events.add(event);
    }

    @Override
    public void publish(String key, BusinessVersion version, OrderUpdatedEvent event) {
      publish(version, event);
    }
  }
}
