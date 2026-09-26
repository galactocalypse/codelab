package com.codelab.orders.service;

import com.codelab.orders.model.CreateCustomerRequest;
import com.codelab.orders.model.CreateCustomerResponse;
import com.codelab.orders.model.GetCustomerResponse;

public interface CustomerService {

  CreateCustomerResponse createCustomer(CreateCustomerRequest request);

  GetCustomerResponse getCustomer(Long id);
}
