package com.codelab.orders.service;

import com.codelab.orders.entity.CustomerEntity;
import com.codelab.orders.exception.CustomerNotFoundException;
import com.codelab.orders.model.CreateCustomerRequest;
import com.codelab.orders.model.CreateCustomerResponse;
import com.codelab.orders.model.GetCustomerResponse;
import com.codelab.orders.repository.CustomerRepository;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class CustomerServiceImpl implements CustomerService {

  private final CustomerRepository repository;

  @Override
  public CreateCustomerResponse createCustomer(CreateCustomerRequest request) {
    return CreateCustomerResponse.from(repository.save(buildEntity(request)));
  }

  @Override
  public GetCustomerResponse getCustomer(Long id) {
    return GetCustomerResponse.from(
        repository
            .findById(id)
            .orElseThrow(
                () -> new CustomerNotFoundException(String.format("Customer %s not found", id))));
  }

  private CustomerEntity buildEntity(CreateCustomerRequest request) {
    return CustomerEntity.builder().name(request.getName()).build();
  }
}
