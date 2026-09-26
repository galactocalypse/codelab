package com.codelab.orders.controller;

import com.codelab.orders.model.CreateProductRequest;
import com.codelab.orders.model.CreateProductResponse;
import com.codelab.orders.model.GetProductResponse;
import com.codelab.orders.service.ProductService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@AllArgsConstructor
@RequestMapping("/api/products")
public class ProductController {

  private final ProductService service;

  @PostMapping
  public CreateProductResponse createProduct(@RequestBody CreateProductRequest request) {
    return service.createProduct(request);
  }

  @GetMapping("/{productId}")
  public GetProductResponse getProduct(@PathVariable("productId") Long id) {
    return service.getProduct(id);
  }
}
