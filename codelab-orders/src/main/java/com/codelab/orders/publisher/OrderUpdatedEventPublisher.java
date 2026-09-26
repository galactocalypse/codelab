package com.codelab.orders.publisher;

import com.codelab.common.spring.eventbus.CodelabEventPublisher;
import com.codelab.common.spring.eventbus.CodelabTopic;
import com.codelab.orders.model.OrderCreatedEvent;
import com.codelab.orders.model.OrderUpdatedEvent;

@CodelabTopic("updated-orders")
public interface OrderUpdatedEventPublisher extends CodelabEventPublisher<OrderUpdatedEvent> {}
