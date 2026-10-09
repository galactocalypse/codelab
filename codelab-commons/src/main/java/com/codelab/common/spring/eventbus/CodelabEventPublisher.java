package com.codelab.common.spring.eventbus;

/**
 * Publishes events onto a Codelab Pulsar topic. The {@link BusinessVersion} is <em>mandated</em>:
 * every publish must declare the business version of the event, which the framework stamps as the
 * {@link CodelabMessageProperties#BUSINESS_VERSION} message property.
 */
public interface CodelabEventPublisher<T> {

  void publish(BusinessVersion version, T event);

  void publish(String key, BusinessVersion version, T event);
}
