package com.codelab.orders.controller;

import com.codelab.orders.model.CreateOrderRequest;
import com.codelab.orders.workflow.OrderProducer;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

  private final OrderProducer producer;

  @PostMapping
  public void createOrder(@RequestBody CreateOrderRequest body) {
    producer.send(body);
  }
}
