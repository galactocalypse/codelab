package com.codelab.todos.model;

import com.codelab.todos.entity.TodoListEntity;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GetTodoListResponse {
  private Long id;
  private String name;
  private LocalDate deadline;
  private List<TodoItemResponse> items;

  public static GetTodoListResponse from(TodoListEntity entity) {
    return GetTodoListResponse.builder()
        .id(entity.getId())
        .name(entity.getName())
        .deadline(entity.getDeadline())
        .items(entity.getItems().stream().map(TodoItemResponse::from).collect(Collectors.toList()))
        .build();
  }
}
