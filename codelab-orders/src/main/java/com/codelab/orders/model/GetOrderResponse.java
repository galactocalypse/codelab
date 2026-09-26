package com.codelab.orders.model;

import com.codelab.orders.entity.OrderEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GetOrderResponse {

  private Long id;
  private Long customerId;
  private OrderStatus status;

  public static GetOrderResponse from(OrderEntity entity) {
    return GetOrderResponse.builder()
        .id(entity.getId())
        .customerId(entity.getCustomerId())
        .status(entity.getStatus())
        .build();
  }
}
