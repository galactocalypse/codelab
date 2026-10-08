package com.codelab.core.eventbus;

import com.codelab.common.spring.eventbus.CodelabMessageProperties;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.apache.pulsar.client.api.Message;
import org.apache.pulsar.client.api.Producer;
import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.PulsarClientException;
import org.apache.pulsar.client.api.Schema;
import org.apache.pulsar.client.api.TypedMessageBuilder;
import org.springframework.beans.factory.DisposableBean;

/**
 * Forwards a message to a dead-letter topic by copying its raw bytes and all of its properties,
 * plus {@link CodelabMessageProperties}' {@code codelab.dlq.*} metadata.
 *
 * <p>The copy is published <em>synchronously</em> and only then does the caller ack the source
 * message: a broker-accepted DLQ write guarantees the message is preserved before its source
 * position is released, so routing never loses a message. On failure the exception propagates, the
 * source message is negative-acked, and the routing is retried on redelivery.
 */
@Slf4j
public class CodelabDlqRouter implements DisposableBean {

  private final PulsarClient pulsarClient;
  private final Map<String, Producer<byte[]>> producers = new ConcurrentHashMap<>();

  public CodelabDlqRouter(PulsarClient pulsarClient) {
    this.pulsarClient = pulsarClient;
  }

  /**
   * Publishes a byte-identical copy of {@code original} to {@code dlqTopic} along with the given
   * metadata, preserving the original key and event time.
   *
   * @throws CodelabDlqRoutingException if the DLQ write does not succeed
   */
  public void publishToDeadLetter(
      String dlqTopic, Message<?> original, Map<String, String> metadata) {
    byte[] payload = original.getData();
    if (payload == null) {
      throw new CodelabDlqRoutingException(
          "Cannot forward null payload from " + original.getMessageId() + " to DLQ " + dlqTopic);
    }
    TypedMessageBuilder<byte[]> builder = producerFor(dlqTopic).newMessage().value(payload);
    original.getProperties().forEach(builder::property);
    metadata.forEach(builder::property);
    String key = original.getKey();
    if (key != null) {
      builder.key(key);
    }
    long eventTime = original.getEventTime();
    if (eventTime > 0) {
      builder.eventTime(eventTime);
    }
    try {
      builder.send();
      log.info(
          "Forwarded message {} ({} bytes) to DLQ {}",
          original.getMessageId(),
          payload.length,
          dlqTopic);
    } catch (PulsarClientException e) {
      throw new CodelabDlqRoutingException(
          "Failed to forward message " + original.getMessageId() + " to DLQ " + dlqTopic, e);
    }
  }

  private Producer<byte[]> producerFor(String dlqTopic) {
    return producers.computeIfAbsent(
        dlqTopic,
        topic -> {
          try {
            return pulsarClient
                .newProducer(Schema.BYTES)
                .topic(topic)
                .enableBatching(false)
                .create();
          } catch (PulsarClientException e) {
            throw new CodelabDlqRoutingException("Failed to create DLQ producer for " + topic, e);
          }
        });
  }

  @Override
  public void destroy() {
    producers
        .values()
        .forEach(
            producer -> {
              try {
                producer.close();
              } catch (PulsarClientException ignored) {
                // closing best-effort on shutdown
              }
            });
  }
}
