package com.codelab.core.eventbus;

import org.apache.pulsar.client.api.Schema;

/** Shared app-level message size check for the publisher invocation handlers. */
final class MessageSizeGuard {

  private MessageSizeGuard() {}

  static <T> void enforce(
      Schema<T> schema, T payload, String topic, String kind, long maxMessageBytes) {
    long actualBytes = schema.encode(payload).length;
    if (actualBytes > maxMessageBytes) {
      throw new CodelabMessageSizeExceededException(kind, topic, actualBytes, maxMessageBytes);
    }
  }
}
