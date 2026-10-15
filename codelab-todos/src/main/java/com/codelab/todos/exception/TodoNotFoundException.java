package com.codelab.todos.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** Thrown when a todo list or item id does not exist; surfaces as HTTP 404. */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class TodoNotFoundException extends RuntimeException {

  public TodoNotFoundException(String message) {
    super(message);
  }
}
