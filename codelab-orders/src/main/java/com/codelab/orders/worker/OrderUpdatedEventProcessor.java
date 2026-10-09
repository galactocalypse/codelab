package com.codelab.orders.worker;

import com.codelab.common.spring.eventbus.BusinessVersion;
import com.codelab.common.spring.eventbus.CodelabEventConsumer;
import com.codelab.common.spring.eventbus.CodelabSubscription;
import com.codelab.orders.model.OrderUpdatedEvent;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@CodelabSubscription(
    topic = "updated-orders",
    subscriptionName = "updated-orders-logger",
    concurrency = 1)
public class OrderUpdatedEventProcessor implements CodelabEventConsumer<OrderUpdatedEvent> {

  @Override
  public Set<BusinessVersion> getSupportedBusinessVersions() {
    // LEGACY covers pre-versioning messages still in flight during rollout.
    return Set.of(BusinessVersion.of("v1"), BusinessVersion.LEGACY);
  }

  @Override
  public void consume(OrderUpdatedEvent event) {
    log.info("Received notification for update of order {}", event.getOrderId());
  }
}
