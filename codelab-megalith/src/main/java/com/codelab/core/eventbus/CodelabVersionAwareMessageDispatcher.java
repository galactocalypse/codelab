package com.codelab.core.eventbus;

import com.codelab.common.spring.eventbus.BusinessVersion;
import com.codelab.common.spring.eventbus.CodelabEventConsumer;
import com.codelab.common.spring.eventbus.CodelabMessageProperties;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.apache.pulsar.client.api.Message;
import org.apache.pulsar.client.api.Schema;
import org.springframework.util.StringUtils;

/**
 * The actual Spring Pulsar listener for a {@link CodelabEventConsumer} subscription.
 *
 * <p>It checks the message's {@link CodelabMessageProperties#BUSINESS_VERSION} from the message
 * properties <em>before</em> deserializing the payload:
 *
 * <ul>
 *   <li>version is supported → decode (with the same {@link Schema#JSON} the publisher uses) and
 *       delegate to {@code consume} — a normal return acks the message, exactly as before;
 *   <li>version is unsupported (or missing when {@link BusinessVersion#LEGACY} is not declared) →
 *       forward a byte-identical copy to the DLQ via {@link CodelabDlqRouter} and return, so the
 *       container acks the source message — <em>no nack, no retry</em>. If the DLQ write fails the
 *       exception propagates and the message is negative-acked and retried later, so a message is
 *       never dropped while being routed.
 * </ul>
 */
@Slf4j
public class CodelabVersionAwareMessageDispatcher<T> {

  private final CodelabEventConsumer<T> delegate;
  private final Set<BusinessVersion> supportedVersions;
  private final String dlqTopic;
  private final String subscriptionName;
  private final CodelabDlqRouter dlqRouter;
  private final Schema<T> schema;

  public CodelabVersionAwareMessageDispatcher(
      CodelabEventConsumer<T> delegate,
      Class<T> payloadType,
      Set<BusinessVersion> supportedVersions,
      String dlqTopic,
      String subscriptionName,
      CodelabDlqRouter dlqRouter) {
    this.delegate = delegate;
    this.supportedVersions = supportedVersions;
    this.dlqTopic = dlqTopic;
    this.subscriptionName = subscriptionName;
    this.dlqRouter = dlqRouter;
    this.schema = Schema.JSON(payloadType);
  }

  public void dispatch(Message<byte[]> message) {
    String raw = message.getProperty(CodelabMessageProperties.BUSINESS_VERSION);
    if (StringUtils.hasText(raw)) {
      try {
        BusinessVersion version = BusinessVersion.of(raw);
        if (supportedVersions.contains(version)) {
          delegate.consume(schema.decode(message.getData()));
          return;
        }
        routeToDeadLetter(
            message,
            CodelabMessageProperties.DLQ_REASON_UNSUPPORTED_BUSINESS_VERSION,
            version.value());
        return;
      } catch (IllegalArgumentException malformed) {
        routeToDeadLetter(
            message, CodelabMessageProperties.DLQ_REASON_UNSUPPORTED_BUSINESS_VERSION, raw.trim());
        return;
      }
    }

    if (supportedVersions.contains(BusinessVersion.LEGACY)) {
      delegate.consume(schema.decode(message.getData()));
      return;
    }
    routeToDeadLetter(
        message,
        CodelabMessageProperties.DLQ_REASON_MISSING_BUSINESS_VERSION,
        BusinessVersion.LEGACY.value());
  }

  private void routeToDeadLetter(Message<byte[]> message, String reason, String observedVersion) {
    Map<String, String> metadata = new LinkedHashMap<>();
    metadata.put(CodelabMessageProperties.DLQ_REASON, reason);
    metadata.put(CodelabMessageProperties.DLQ_OBSERVED_VERSION, observedVersion);
    metadata.put(CodelabMessageProperties.DLQ_CONSUMER, delegate.getClass().getName());
    metadata.put(CodelabMessageProperties.DLQ_ORIGINAL_TOPIC, message.getTopicName());
    metadata.put(CodelabMessageProperties.DLQ_ORIGINAL_SUBSCRIPTION, subscriptionName);
    metadata.put(
        CodelabMessageProperties.DLQ_ORIGINAL_MESSAGE_ID, message.getMessageId().toString());
    metadata.put(CodelabMessageProperties.DLQ_ROUTED_AT, Instant.now().toString());

    dlqRouter.publishToDeadLetter(dlqTopic, message, metadata);
    log.warn(
        "Routed message {} (business version {}) to DLQ {} for consumer {} — {}",
        message.getMessageId(),
        observedVersion,
        dlqTopic,
        delegate.getClass().getSimpleName(),
        reason);
  }
}
