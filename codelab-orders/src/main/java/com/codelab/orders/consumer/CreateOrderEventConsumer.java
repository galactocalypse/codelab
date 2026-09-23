package com.codelab.orders.consumer;

import com.codelab.common.spring.eventbus.CodelabEventConsumer;
import com.codelab.common.spring.eventbus.CodelabSubscription;
import com.codelab.orders.model.CreateOrderEvent;
import org.springframework.pulsar.annotation.PulsarListener;

@CodelabSubscription(
        topic = "orders",
        subscriptionName = "codelab-orders"
)
public interface CreateOrderEventConsumer extends CodelabEventConsumer<CreateOrderEvent> {
}
