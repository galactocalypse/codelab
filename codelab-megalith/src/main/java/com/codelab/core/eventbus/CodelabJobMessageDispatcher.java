package com.codelab.core.eventbus;

import com.codelab.common.spring.jobbus.CodelabJobConsumer;
import org.apache.pulsar.client.api.Message;
import org.apache.pulsar.client.api.Schema;

/**
 * The actual Spring Pulsar listener for a {@link CodelabJobConsumer} subscription — the job-path
 * counterpart of {@link CodelabVersionAwareMessageDispatcher}, without any business-version gate.
 *
 * <p>The payload is decoded straight with {@link Schema#JSON} and delegated. Any failure — decode
 * or {@code consume} — propagates, so the container negative-acks the message and Pulsar's native
 * retry takes over: redelivery per the {@code DeadLetterPolicy}, then the DLQ. The framework never
 * intercepts a job message after it has been handed to the listener.
 */
public class CodelabJobMessageDispatcher<T> {

  private final CodelabJobConsumer<T> delegate;
  private final Schema<T> schema;

  public CodelabJobMessageDispatcher(CodelabJobConsumer<T> delegate, Class<T> payloadType) {
    this.delegate = delegate;
    this.schema = Schema.JSON(payloadType);
  }

  public void dispatch(Message<byte[]> message) {
    delegate.consume(schema.decode(message.getData()));
  }
}
