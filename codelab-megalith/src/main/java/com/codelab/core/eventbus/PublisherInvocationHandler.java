package com.codelab.core.eventbus;

import com.codelab.common.spring.eventbus.BusinessVersion;
import com.codelab.common.spring.eventbus.CodelabMessageProperties;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import lombok.AllArgsConstructor;
import org.apache.pulsar.client.api.Producer;

@AllArgsConstructor
class PublisherInvocationHandler<E> implements InvocationHandler {

  private final Producer<E> producer;
  private final Class<E> payloadType;

  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    // Object-class methods (equals/hashCode/toString) hit the proxy itself,
    // not your single business method — handle them or every log statement
    // touching this bean will throw
    if (method.getDeclaringClass() == Object.class) {
      return method.invoke(this, args);
    }

    if ("publish".equals(method.getName())) {
      if (args == null) {
        throw new IllegalArgumentException("Missing publish arguments");
      }

      if (args.length == 2 && args[0] instanceof BusinessVersion version) {
        E event = validatePayload(args[1]);
        return producer
            .newMessage()
            .property(CodelabMessageProperties.BUSINESS_VERSION, version.value())
            .value(event)
            .send();
      }

      if (args.length == 3
          && args[0] instanceof String key
          && args[1] instanceof BusinessVersion version) {
        E event = validatePayload(args[2]);
        return producer
            .newMessage()
            .property(CodelabMessageProperties.BUSINESS_VERSION, version.value())
            .key(key)
            .value(event)
            .send();
      }
    }

    throw new UnsupportedOperationException("Unexpected method on publisher proxy: " + method);
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
