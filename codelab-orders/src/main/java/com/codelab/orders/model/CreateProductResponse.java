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
public class CreateProductResponse {
  private Long id;

  public static CreateProductResponse from(ProductEntity entity) {
    return CreateProductResponse.builder().id(entity.getId()).build();
  }
}
