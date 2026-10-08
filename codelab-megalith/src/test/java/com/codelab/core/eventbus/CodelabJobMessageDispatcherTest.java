package com.codelab.core.eventbus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.codelab.common.spring.jobbus.CodelabJobConsumer;
import java.nio.charset.StandardCharsets;
import org.apache.pulsar.client.api.Message;
import org.apache.pulsar.client.api.Schema;
import org.junit.jupiter.api.Test;

class CodelabJobMessageDispatcherTest {

  @SuppressWarnings("unchecked")
  private final Message<byte[]> message = mock(Message.class);

  @Test
  void decodesThePayloadWithJsonSchemaAndDelegates() {
    RecordingConsumer delegate = new RecordingConsumer();
    CodelabJobMessageDispatcher<String> dispatcher =
        new CodelabJobMessageDispatcher<>(delegate, String.class);

    byte[] payload = Schema.JSON(String.class).encode("job-payload");
    when(message.getData()).thenReturn(payload);

    dispatcher.dispatch(message);

    assertThat(delegate.received).containsExactly("job-payload");
  }

  @Test
  void propagatesConsumerFailuresSoTheContainerNegativeAcks() {
    RuntimeException failure = new IllegalStateException("boom");
    CodelabJobConsumer<String> failing =
        job -> {
          throw failure;
        };
    CodelabJobMessageDispatcher<String> dispatcher =
        new CodelabJobMessageDispatcher<>(failing, String.class);

    when(message.getData()).thenReturn(Schema.JSON(String.class).encode("job-payload"));

    // No wrapping and no swallowing: Pulsar's listener container sees the failure,
    // negative-acks, and the DeadLetterPolicy's redelivery budget takes over.
    assertThatThrownBy(() -> dispatcher.dispatch(message)).isSameAs(failure);
  }

  @Test
  void propagatesDecodeFailuresSoTheContainerNegativeAcks() {
    RecordingConsumer delegate = new RecordingConsumer();
    CodelabJobMessageDispatcher<String> dispatcher =
        new CodelabJobMessageDispatcher<>(delegate, String.class);

    when(message.getData()).thenReturn("not-json".getBytes(StandardCharsets.UTF_8));

    assertThatThrownBy(() -> dispatcher.dispatch(message)).isInstanceOf(RuntimeException.class);
    assertThat(delegate.received).isEmpty();
  }

  private static final class RecordingConsumer implements CodelabJobConsumer<String> {
    private final java.util.List<String> received = new java.util.ArrayList<>();

    @Override
    public void consume(String job) {
      received.add(job);
    }
  }
}
