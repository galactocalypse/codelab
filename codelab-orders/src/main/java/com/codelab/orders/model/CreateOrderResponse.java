package com.codelab.orders.model;

import com.codelab.orders.entity.OrderEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreateOrderResponse {

  private Long id;
  private OrderStatus status;

  public static CreateOrderResponse from(OrderEntity entity) {
    CreateOrderResponse.CreateOrderResponseBuilder builder = CreateOrderResponse.builder();
    return builder.id(entity.getId()).status(entity.getStatus()).build();
  }
}
