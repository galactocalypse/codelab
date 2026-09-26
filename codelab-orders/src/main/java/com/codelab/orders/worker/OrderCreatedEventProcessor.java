package com.codelab.orders.worker;

import com.codelab.common.spring.eventbus.CodelabEventConsumer;
import com.codelab.common.spring.eventbus.CodelabSubscription;
import com.codelab.orders.model.OrderCreatedEvent;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@CodelabSubscription(
    topic = "created-orders",
    subscriptionName = "created-orders-logger",
    concurrency = 1)
public class OrderCreatedEventProcessor implements CodelabEventConsumer<OrderCreatedEvent> {

  @Override
  public void consume(OrderCreatedEvent event) {
    log.info("Received notification for creation of order {}", event.getOrderId());
  }
}
