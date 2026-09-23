package com.codelab.orders.publisher;

import com.codelab.common.spring.eventbus.CodelabEventPublisher;
import com.codelab.common.spring.eventbus.CodelabTopic;
import com.codelab.orders.model.CreateOrderEvent;


@CodelabTopic(name = "orders", value = "orders")
public interface CreateOrderEventPublisher extends CodelabEventPublisher<CreateOrderEvent> {
}
