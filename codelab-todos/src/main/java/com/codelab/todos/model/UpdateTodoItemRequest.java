package com.codelab.todos.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTodoItemRequest {
  private String description;
  private String notes;
  private Boolean done;
}
