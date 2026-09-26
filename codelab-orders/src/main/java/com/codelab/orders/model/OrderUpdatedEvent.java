package com.codelab.orders.model;

import com.codelab.orders.entity.OrderEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.pulsar.common.schema.SchemaType;
import org.springframework.pulsar.annotation.PulsarMessage;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@PulsarMessage(schemaType = SchemaType.JSON)
public class OrderUpdatedEvent {

  private Long orderId;

  public static OrderUpdatedEvent from(OrderEntity request) {
    return OrderUpdatedEvent.builder().orderId(request.getId()).build();
  }
}
