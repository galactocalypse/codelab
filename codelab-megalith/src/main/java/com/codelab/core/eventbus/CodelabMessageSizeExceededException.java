package com.codelab.core.eventbus;

/**
 * Thrown by the publisher path when the encoded payload exceeds the configured app-level message
 * size cap ({@code app.pulsar.events.max-message-size} for events, {@code
 * app.pulsar.jobs.max-message-size} for jobs). The publish is rejected <em>before</em> anything is
 * handed to the Pulsar producer, so an oversized message never reaches the broker.
 */
public class CodelabMessageSizeExceededException extends RuntimeException {

  private final String topic;
  private final long actualBytes;
  private final long maxBytes;

  public CodelabMessageSizeExceededException(
      String kind, String topic, long actualBytes, long maxBytes) {
    super(
        "%s payload for topic %s encodes to %d bytes, exceeding the %d-byte publish limit"
            .formatted(kind, topic, actualBytes, maxBytes));
    this.topic = topic;
    this.actualBytes = actualBytes;
    this.maxBytes = maxBytes;
  }

  public String getTopic() {
    return topic;
  }

  public long getActualBytes() {
    return actualBytes;
  }

  public long getMaxBytes() {
    return maxBytes;
  }
}
