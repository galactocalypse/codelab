package com.codelab.todos.model;

import com.codelab.todos.entity.TodoItemEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TodoItemResponse {
  private Long id;
  private String description;
  private String notes;
  private Boolean done;
  private Long todoListId;

  public static TodoItemResponse from(TodoItemEntity entity) {
    return TodoItemResponse.builder()
        .id(entity.getId())
        .description(entity.getDescription())
        .notes(entity.getNotes())
        .done(entity.getDone())
        .todoListId(entity.getTodoList() != null ? entity.getTodoList().getId() : null)
        .build();
  }
}
