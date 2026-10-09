package com.codelab.common.spring.jobbus;

/**
 * Publishes jobs onto a Codelab Pulsar topic. Jobs are heavy async units of work (file imports,
 * batch imports, backfills) — as opposed to {@link com.codelab.common.spring.eventbus
 * .CodelabEventPublisher} events, jobs carry <em>no business version</em>: their payload contract
 * is owned end-to-end by the producing and consuming application code, and failure handling is
 * delegated to Pulsar's native retry + {@code DeadLetterPolicy}.
 */
public interface CodelabJobPublisher<T> {

  void publish(T job);

  void publish(String key, T job);
}
