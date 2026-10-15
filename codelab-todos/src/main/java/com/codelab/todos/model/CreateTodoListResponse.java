package com.codelab.todos.model;

import com.codelab.todos.entity.TodoListEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateTodoListResponse {
  private Long id;
  private String name;

  public static CreateTodoListResponse from(TodoListEntity entity) {
    return CreateTodoListResponse.builder().id(entity.getId()).name(entity.getName()).build();
  }
}
