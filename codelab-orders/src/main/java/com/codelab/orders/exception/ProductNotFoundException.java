package com.codelab.orders.exception;

import java.io.Serial;

public class ProductNotFoundException extends RuntimeException {

  @Serial private static final long serialVersionUID = 1L;

  public ProductNotFoundException(String message) {
    super(message);
  }
}
