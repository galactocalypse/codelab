package com.codelab.orders.service;

import com.codelab.orders.entity.OrderEntity;
import com.codelab.orders.entity.ProductEntity;
import com.codelab.orders.exception.OrderNotFoundException;
import com.codelab.orders.model.*;
import com.codelab.orders.repository.OrderRepository;
import com.codelab.orders.repository.ProductRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import lombok.AllArgsConstructor;

/**
 * Order state lives here; order <em>events</em> do not. State changes reach the event bus through
 * CDC (Debezium captures the {@code orders} table, {@link
 * com.codelab.orders.cdc.OrderCdcNormalizer} turns row changes into versioned events), so this
 * service persists and nothing else — no publisher imports, no business-version knowledge.
 */
@AllArgsConstructor
public class OrderServiceImpl implements OrderService {

  private final OrderRepository repository;
  private final ProductRepository productRepository;

  public CreateOrderResponse createOrder(CreateOrderRequest request) {
    OrderEntity createdOrder = repository.save(buildEntity(request));
    return CreateOrderResponse.from(createdOrder);
  }

  @Override
  public UpdateOrderResponse updateStatus(Long orderId, UpdateOrderRequest request) {
    OrderEntity order =
        repository
            .findById(orderId)
            .orElseThrow(
                () -> new OrderNotFoundException(String.format("Order %s not found", orderId)));
    order.setStatus(request.getTargetStatus());
    return UpdateOrderResponse.from(repository.save(order));
  }

  @Override
  public GetOrderResponse getOrder(Long id) {
    return GetOrderResponse.from(
        repository
            .findById(id)
            .orElseThrow(
                () -> new OrderNotFoundException(String.format("Order %s not found", id))));
  }

  private OrderEntity buildEntity(CreateOrderRequest request) {
    OrderEntity order =
        OrderEntity.builder()
            .createdAtDate(LocalDate.now())
            .createdAtTime(LocalTime.now())
            .status(OrderStatus.CREATED)
            .build();
    for (CreateOrderRequest.LineItem item : request.getLineItems()) {
      ProductEntity product = productRepository.getReferenceById(item.getProductId());
      order.addLineItem(product, item.getCount());
    }
    return order;
  }
}
