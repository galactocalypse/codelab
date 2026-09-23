package com.codelab.orders.workflow;

import com.codelab.orders.model.CreateOrderEvent;
import com.codelab.orders.model.CreateOrderRequest;
import com.codelab.orders.publisher.CreateOrderEventPublisher;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Date;

@Slf4j
@Service
@AllArgsConstructor
public class OrderProducer {

  private final CreateOrderEventPublisher createOrderEventPublisher;

  public void send(CreateOrderRequest request) {
    createOrderEventPublisher.publish(CreateOrderEvent.from(request));
    log.info("Message published at {}", new Date());
  }

}
