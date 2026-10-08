package com.codelab.core.eventbus;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.codelab.common.spring.eventbus.CodelabMessageProperties;
import java.util.Map;
import org.apache.pulsar.client.api.Message;
import org.apache.pulsar.client.api.MessageId;
import org.apache.pulsar.client.api.Producer;
import org.apache.pulsar.client.api.ProducerBuilder;
import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.PulsarClientException;
import org.apache.pulsar.client.api.Schema;
import org.apache.pulsar.client.api.TypedMessageBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CodelabDlqRouterTest {

  private static final String DLQ = "movies-events-created-movie-logger-DLQ";

  private PulsarClient pulsarClient;

  @SuppressWarnings("unchecked")
  private ProducerBuilder<byte[]> producerBuilder;

  @SuppressWarnings("unchecked")
  private Producer<byte[]> dlqProducer;

  @SuppressWarnings("unchecked")
  private TypedMessageBuilder<byte[]> builder;

  private CodelabDlqRouter router;

  @SuppressWarnings("unchecked")
  private Message<byte[]> original;

  @SuppressWarnings("unchecked")
  private final MessageId messageId = mock(MessageId.class);

  private final byte[] payload = new byte[] {1, 2, 3};

  @BeforeEach
  @SuppressWarnings("unchecked")
  void setUp() throws Exception {
    pulsarClient = mock(PulsarClient.class);
    producerBuilder = (ProducerBuilder<byte[]>) mock(ProducerBuilder.class);
    dlqProducer = mock(Producer.class);
    builder = mock(TypedMessageBuilder.class);

    when(pulsarClient.newProducer(any(Schema.class))).thenReturn(producerBuilder);
    when(producerBuilder.topic(any(String.class))).thenReturn(producerBuilder);
    when(producerBuilder.enableBatching(false)).thenReturn(producerBuilder);
    when(producerBuilder.create()).thenReturn(dlqProducer);
    when(dlqProducer.newMessage()).thenReturn(builder);
    when(builder.value(same(payload))).thenReturn(builder);
    when(builder.property(any(), any())).thenReturn(builder);
    when(builder.key(any())).thenReturn(builder);
    when(builder.eventTime(anyLong())).thenReturn(builder);
    when(builder.send()).thenReturn(messageId);

    router = new CodelabDlqRouter(pulsarClient);

    original = mock(Message.class);
    when(original.getData()).thenReturn(payload);
    when(original.getMessageId()).thenReturn(messageId);
    when(original.getKey()).thenReturn("m-1");
    when(original.getEventTime()).thenReturn(42L);
    when(original.getProperties())
        .thenReturn(Map.of(CodelabMessageProperties.BUSINESS_VERSION, "v1"));
  }

  @Test
  void copiesPayloadPropertiesKeyAndEventTimeAndForwards() throws Exception {
    router.publishToDeadLetter(
        DLQ,
        original,
        Map.of(
            CodelabMessageProperties.DLQ_REASON, "UNSUPPORTED_BUSINESS_VERSION",
            CodelabMessageProperties.DLQ_CONSUMER, "MovieProcessor"));

    verify(pulsarClient).newProducer(Schema.BYTES);
    verify(producerBuilder).enableBatching(false);
    verify(builder).value(same(payload));
    verify(builder).property(CodelabMessageProperties.BUSINESS_VERSION, "v1");
    verify(builder).property(CodelabMessageProperties.DLQ_REASON, "UNSUPPORTED_BUSINESS_VERSION");
    verify(builder).property(CodelabMessageProperties.DLQ_CONSUMER, "MovieProcessor");
    verify(builder).key("m-1");
    verify(builder).eventTime(42L);
    verify(builder).send();
  }

  @Test
  void createsTheProducerOncePerTopic() throws Exception {
    router.publishToDeadLetter(DLQ, original, Map.of());
    router.publishToDeadLetter(DLQ, original, Map.of());
    router.publishToDeadLetter("movies-events-updated-movie-logger-DLQ", original, Map.of());

    verify(producerBuilder, times(2)).create();
  }

  @Test
  void propagatesSendFailuresSoTheSourceMessageIsRetriedNotLost() throws Exception {
    when(builder.send()).thenThrow(new PulsarClientException.BrokerPersistenceException("down"));

    assertThatThrownBy(() -> router.publishToDeadLetter(DLQ, original, Map.of()))
        .isInstanceOf(CodelabDlqRoutingException.class)
        .hasRootCauseInstanceOf(PulsarClientException.class);
  }

  @Test
  void rejectsNullPayloads() {
    when(original.getData()).thenReturn(null);

    assertThatThrownBy(() -> router.publishToDeadLetter(DLQ, original, Map.of()))
        .isInstanceOf(CodelabDlqRoutingException.class)
        .hasMessageContaining("null payload");

    verify(dlqProducer, never()).newMessage();
  }
}
