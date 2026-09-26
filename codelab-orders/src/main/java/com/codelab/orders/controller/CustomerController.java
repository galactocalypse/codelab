package com.codelab.orders.controller;

import com.codelab.orders.model.CreateCustomerRequest;
import com.codelab.orders.model.CreateCustomerResponse;
import com.codelab.orders.model.GetCustomerResponse;
import com.codelab.orders.service.CustomerService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@AllArgsConstructor
@RequestMapping("/api/customers")
public class CustomerController {

  private final CustomerService service;

  @PostMapping
  public CreateCustomerResponse createCustomer(@RequestBody CreateCustomerRequest request) {
    return service.createCustomer(request);
  }

  @GetMapping("/{customerId}")
  public GetCustomerResponse getCustomer(@PathVariable("customerId") Long id) {
    return service.getCustomer(id);
  }
}
