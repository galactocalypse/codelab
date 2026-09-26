package com.codelab.orders.controller;

import com.codelab.orders.model.*;
import com.codelab.orders.service.OrderServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

  private final OrderServiceImpl service;

  @PostMapping
  public CreateOrderResponse createOrder(@RequestBody CreateOrderRequest body) {
    return service.createOrder(body);
  }

  @GetMapping("/{orderId}")
  public GetOrderResponse getOrder(@PathVariable("orderId") Long orderId) {
    return service.getOrder(orderId);
  }

  @PutMapping("/{orderId}")
  public UpdateOrderResponse updateOrderStatus(
      @PathVariable("orderId") Long orderId, @RequestBody UpdateOrderRequest request) {
    return service.updateStatus(orderId, request);
  }
}
