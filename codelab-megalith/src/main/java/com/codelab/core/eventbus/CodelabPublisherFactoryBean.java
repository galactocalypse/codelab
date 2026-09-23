package com.codelab.core.eventbus;

import com.codelab.common.spring.eventbus.CodelabEventPublisher;
import lombok.RequiredArgsConstructor;
import org.apache.pulsar.client.api.Producer;
import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.Schema;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.reflect.Proxy;

@RequiredArgsConstructor
public class CodelabPublisherFactoryBean<T extends CodelabEventPublisher<?>>
    implements FactoryBean<T>, InitializingBean, DisposableBean {

  private final Class<T> publisherInterface;
  private final String resolvedTopic;
  private final Class<?> payloadType;

  @Autowired
  private PulsarClient pulsarClient; // injected normally, since this bean IS in the app context

  private Producer<?> producer;
  private T proxy;

  @Override
  public void afterPropertiesSet() throws Exception {
    Schema<?> schema = Schema.JSON(payloadType); // or your schema-resolution strategy
    producer = pulsarClient.newProducer(schema).topic(resolvedTopic).create();

    proxy =
        (T)
            Proxy.newProxyInstance(
                publisherInterface.getClassLoader(),
                new Class<?>[] {publisherInterface},
                new PublisherInvocationHandler((Producer<Object>) producer));
  }

  @Override
  public T getObject() {
    return proxy;
  }

  @Override
  public Class<?> getObjectType() {
    return publisherInterface;
  }

  @Override
  public boolean isSingleton() {
    return true;
  }

  @Override
  public void destroy() throws Exception {
    if (producer != null) producer.close();
  }
}
