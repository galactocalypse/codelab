package com.codelab.orders.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.pulsar.common.schema.SchemaType;
import org.springframework.pulsar.annotation.PulsarMessage;

@Data
@NoArgsConstructor
@AllArgsConstructor
@PulsarMessage(schemaType = SchemaType.JSON)
public class CreateOrderEvent {
  private String customerId;

  public static CreateOrderEvent from(CreateOrderRequest request) {
    return new CreateOrderEvent(request.getCustomerId());
  }
}
