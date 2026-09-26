package com.codelab.orders.publisher;

import com.codelab.common.spring.eventbus.CodelabEventPublisher;
import com.codelab.common.spring.eventbus.CodelabTopic;
import com.codelab.orders.model.OrderCreatedEvent;

@CodelabTopic("created-orders")
public interface OrderCreatedEventPublisher extends CodelabEventPublisher<OrderCreatedEvent> {}
