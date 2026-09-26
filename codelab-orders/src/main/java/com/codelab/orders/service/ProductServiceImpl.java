package com.codelab.orders.service;

import com.codelab.orders.entity.ProductEntity;
import com.codelab.orders.exception.ProductNotFoundException;
import com.codelab.orders.model.CreateProductRequest;
import com.codelab.orders.model.CreateProductResponse;
import com.codelab.orders.model.GetProductResponse;
import com.codelab.orders.repository.ProductRepository;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class ProductServiceImpl implements ProductService {

  private final ProductRepository repository;

  @Override
  public CreateProductResponse createProduct(CreateProductRequest request) {
    return CreateProductResponse.from(repository.save(buildEntity(request)));
  }

  @Override
  public GetProductResponse getProduct(Long id) {
    return GetProductResponse.from(
        repository
            .findById(id)
            .orElseThrow(
                () -> new ProductNotFoundException(String.format("Product %s not found", id))));
  }

  private static ProductEntity buildEntity(CreateProductRequest request) {
    return ProductEntity.builder().name(request.getName()).build();
  }
}
