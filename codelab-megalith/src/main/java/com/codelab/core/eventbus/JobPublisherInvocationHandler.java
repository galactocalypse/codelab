package com.codelab.core.eventbus;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import lombok.AllArgsConstructor;
import org.apache.pulsar.client.api.Producer;
import org.apache.pulsar.client.api.PulsarClientException;
import org.apache.pulsar.client.api.Schema;

/**
 * Invocation handler for {@link com.codelab.common.spring.jobbus.CodelabJobPublisher} proxies.
 * Unlike {@link PublisherInvocationHandler} it stamps no business-version property — jobs are not
 * version-gated — but it enforces the job-side app message size cap before the send.
 */
@AllArgsConstructor
class JobPublisherInvocationHandler<E> implements InvocationHandler {

  private final Producer<E> producer;
  private final Class<E> payloadType;
  private final Schema<E> schema;
  private final String topic;
  private final long maxMessageBytes;

  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    // Object-class methods (equals/hashCode/toString) hit the proxy itself,
    // not your single business method — handle them or every log statement
    // touching this bean will throw
    if (method.getDeclaringClass() == Object.class) {
      return method.invoke(this, args);
    }

    if ("publish".equals(method.getName())) {
      if (args == null || args.length == 0) {
        throw new IllegalArgumentException("Missing publish arguments");
      }

      if (args.length == 1) {
        return send(null, validatePayload(args[0]));
      }

      if (args.length == 2 && args[0] instanceof String key) {
        return send(key, validatePayload(args[1]));
      }
    }

    throw new UnsupportedOperationException("Unexpected method on job publisher proxy: " + method);
  }

  private Object send(String key, E job) throws PulsarClientException {
    MessageSizeGuard.enforce(schema, job, topic, "Job", maxMessageBytes);
    var builder = producer.newMessage().value(job);
    if (key != null) {
      builder.key(key);
    }
    return builder.send();
  }

  @SuppressWarnings("unchecked")
  private E validatePayload(Object value) {
    if (!payloadType.isInstance(value)) {
      throw new IllegalArgumentException(
          "Expected "
              + payloadType.getName()
              + " but got "
              + (value == null ? "null" : value.getClass().getName()));
    }

    return (E) value;
  }
}
