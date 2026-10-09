package com.codelab.common.spring.eventbus;

import java.util.Set;

/**
 * Consumes events from a Codelab Pulsar topic. A consumer declares which {@link BusinessVersion}s
 * it can handle in {@link #getSupportedBusinessVersions()}; the framework routes any other version
 * directly to the DLQ, before {@link #consume(Object)} is ever invoked.
 */
public interface CodelabEventConsumer<T> {

  /** The exact business versions this consumer can parse. Read once at startup. */
  Set<BusinessVersion> getSupportedBusinessVersions();

  /**
   * Handler for a single message. Unsupported versions never reach this method — the framework
   * routes them to the DLQ before deserialization.
   */
  void consume(T event);
}
