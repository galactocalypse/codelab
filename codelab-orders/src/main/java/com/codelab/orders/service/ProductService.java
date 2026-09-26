package com.codelab.orders.service;

import com.codelab.orders.model.CreateProductRequest;
import com.codelab.orders.model.CreateProductResponse;
import com.codelab.orders.model.GetProductResponse;

public interface ProductService {

  CreateProductResponse createProduct(CreateProductRequest request);

  GetProductResponse getProduct(Long id);
}
