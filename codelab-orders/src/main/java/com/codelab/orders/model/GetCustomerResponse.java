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
public class GetCustomerResponse {

  private Long id;
  private String name;

  public static GetCustomerResponse from(CustomerEntity entity) {
    return GetCustomerResponse.builder().id(entity.getId()).name(entity.getName()).build();
  }
}
