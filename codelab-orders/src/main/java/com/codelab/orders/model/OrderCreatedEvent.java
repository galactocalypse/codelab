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
public class OrderCreatedEvent {

  private Long orderId;

  public static OrderCreatedEvent from(OrderEntity request) {
    return OrderCreatedEvent.builder().orderId(request.getId()).build();
  }
}
