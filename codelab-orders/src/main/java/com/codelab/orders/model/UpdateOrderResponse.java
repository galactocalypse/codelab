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
public class UpdateOrderResponse {

  private OrderStatus status;

  public static UpdateOrderResponse from(OrderEntity entity) {
    return UpdateOrderResponse.builder().status(entity.getStatus()).build();
  }
}
