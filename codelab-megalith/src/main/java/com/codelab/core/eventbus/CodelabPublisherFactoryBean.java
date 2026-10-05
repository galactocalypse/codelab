package com.codelab.core.eventbus;

import com.codelab.common.spring.eventbus.CodelabEventPublisher;
import java.lang.reflect.Proxy;
import lombok.RequiredArgsConstructor;
import org.apache.pulsar.client.api.Producer;
import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.Schema;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.pulsar.core.PulsarProducerFactory;
import org.springframework.pulsar.core.PulsarTemplate;

@RequiredArgsConstructor
public class CodelabPublisherFactoryBean<E, T extends CodelabEventPublisher<E>>
    implements FactoryBean<T>, InitializingBean, DisposableBean {

  private final Class<T> publisherInterface;
  private final String resolvedTopic;
  private final Class<E> payloadType;

  @Autowired
  private PulsarClient pulsarClient; // injected normally, since this bean IS in the app context

  private Producer<E> producer;
  private T proxy;

  @Override
  @SuppressWarnings("unchecked")
  public void afterPropertiesSet() throws Exception {
    Schema<E> schema = Schema.JSON(payloadType); // or your schema-resolution strategy
    producer = pulsarClient.newProducer(schema).topic(resolvedTopic).create();

    proxy =
        (T)
            Proxy.newProxyInstance(
                publisherInterface.getClassLoader(),
                new Class<?>[] {publisherInterface},
                new PublisherInvocationHandler<>(producer, payloadType));
  }

  @Override
  public T getObject() {
    return proxy;
  }

  @Override
  public Class<T> getObjectType() {
    return publisherInterface;
  }

  @Override
  public void destroy() throws Exception {
    if (producer != null) producer.close();
  }
}
