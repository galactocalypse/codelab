package com.codelab.orders.exception;

import java.io.Serial;

public class OrderNotFoundException extends RuntimeException {

  @Serial private static final long serialVersionUID = 1L;

  public OrderNotFoundException(String message) {
    super(message);
  }
}
