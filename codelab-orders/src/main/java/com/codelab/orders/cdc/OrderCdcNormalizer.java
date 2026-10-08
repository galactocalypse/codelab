package com.codelab.orders.cdc;

import com.codelab.common.spring.eventbus.BusinessVersion;
import com.codelab.common.spring.jobbus.CodelabJobConsumer;
import com.codelab.common.spring.jobbus.CodelabJobSubscription;
import com.codelab.orders.model.OrderCreatedEvent;
import com.codelab.orders.model.OrderUpdatedEvent;
import com.codelab.orders.publisher.OrderCreatedEventPublisher;
import com.codelab.orders.publisher.OrderUpdatedEventPublisher;
import lombok.extern.slf4j.Slf4j;
import org.apache.pulsar.client.api.SubscriptionInitialPosition;

/**
 * Turns raw {@code orders} table row changes — captured by Debezium from Postgres logical
 * replication — into versioned domain events.
 *
 * <p>This is the single point where the orders module produces business events: services persist
 * state and never publish, CDC delivers the change, and this normalizer stamps {@code v1} and hands
 * the event to the event bus (from where the framework's version gate governs consumption).
 *
 * <p>Consumed as a <em>job</em> (no business version on the wire): a malformed envelope
 * negative-acks and, after the {@code orderCdcDeadLetterPolicy} redelivery budget, parks on the DLQ
 * rather than blocking the stream. Deletes and snapshot reads ({@code d}/{@code r}) produce no
 * event — the event contract only covers creation and update.
 */
@Slf4j
@CodelabJobSubscription(
    topic = "orders-cdc.public.orders",
    subscriptionName = "orders-cdc-normalizer",
    initialPosition = SubscriptionInitialPosition.Earliest,
    deadLetterPolicyRef = "orderCdcDeadLetterPolicy")
public class OrderCdcNormalizer implements CodelabJobConsumer<CdcOrderChange> {

  private static final BusinessVersion EVENT_VERSION = BusinessVersion.of("v1");

  private final OrderCreatedEventPublisher createdPublisher;
  private final OrderUpdatedEventPublisher updatedPublisher;

  public OrderCdcNormalizer(
      OrderCreatedEventPublisher createdPublisher, OrderUpdatedEventPublisher updatedPublisher) {
    this.createdPublisher = createdPublisher;
    this.updatedPublisher = updatedPublisher;
  }

  @Override
  public void consume(CdcOrderChange change) {
    if (change == null || change.getOp() == null || change.getAfter() == null) {
      return;
    }
    Long orderId = change.getAfter().getId();
    if (orderId == null) {
      log.debug("Ignoring CDC change without an order id (op={})", change.getOp());
      return;
    }

    switch (change.getOp()) {
      case "c" ->
          createdPublisher.publish(
              Long.toString(orderId),
              EVENT_VERSION,
              OrderCreatedEvent.builder().orderId(orderId).build());
      case "u" ->
          updatedPublisher.publish(
              EVENT_VERSION, OrderUpdatedEvent.builder().orderId(orderId).build());
      default -> log.debug("Ignoring CDC op '{}' for order {}", change.getOp(), orderId);
    }
  }
}
