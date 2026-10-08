package com.codelab.common.spring.jobbus;

/**
 * Consumes jobs from a Codelab Pulsar topic. Unlike {@link com.codelab.common.spring.eventbus
 * .CodelabEventConsumer}, a job consumer declares no business versions — messages are decoded
 * straight into the payload type and handed to {@link #consume(Object)}.
 *
 * <p>Failure handling is Pulsar-native: a thrown exception negative-acks the message, redelivery
 * follows the listener's {@code DeadLetterPolicy} (see {@code deadLetterPolicyRef} on {@link
 * CodelabJobSubscription}), and messages are parked on the DLQ after the configured redelivery
 * count. There is no framework version gate and no framework DLQ routing on this path.
 */
public interface CodelabJobConsumer<T> {

  /** Handler for a single job. Throwing negative-acks the message for redelivery. */
  void consume(T job);
}
