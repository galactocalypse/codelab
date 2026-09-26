package com.codelab.orders.model;

import com.codelab.orders.entity.CustomerEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCustomerResponse {
  private Long id;

  public static CreateCustomerResponse from(CustomerEntity entity) {
    return CreateCustomerResponse.builder().id(entity.getId()).build();
  }
}
