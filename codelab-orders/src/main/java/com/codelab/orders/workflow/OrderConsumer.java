package com.codelab.orders.workflow;

import com.codelab.common.spring.eventbus.CodelabEventConsumer;
import com.codelab.common.spring.eventbus.CodelabSubscription;
import com.codelab.orders.model.CreateOrderEvent;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@CodelabSubscription(topic = "orders", subscriptionName = "codelab-orders", concurrency = 1)
public class OrderConsumer implements CodelabEventConsumer<CreateOrderEvent> {

  @Override
  public void consume(CreateOrderEvent event) {
    log.info("Received order from: {}", event.getCustomerId());
  }
}
