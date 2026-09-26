package com.codelab.orders.model;

import com.codelab.orders.entity.ProductEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GetProductResponse {
  private Long id;
  private String name;

  public static GetProductResponse from(ProductEntity entity) {
    return GetProductResponse.builder().id(entity.getId()).name(entity.getName()).build();
  }
}
