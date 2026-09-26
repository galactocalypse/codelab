package com.codelab.orders.service;

import com.codelab.orders.model.*;

public interface OrderService {

  CreateOrderResponse createOrder(CreateOrderRequest request);

  UpdateOrderResponse updateStatus(Long id, UpdateOrderRequest request);

  GetOrderResponse getOrder(Long orderId);
}
