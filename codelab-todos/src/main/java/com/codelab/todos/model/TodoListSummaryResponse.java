package com.codelab.todos.model;

import com.codelab.todos.entity.TodoListEntity;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Lightweight view of a todo list used when listing many lists (no items hydrated). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TodoListSummaryResponse {
  private Long id;
  private String name;
  private LocalDate deadline;
  private int itemCount;
  private int doneCount;

  public static TodoListSummaryResponse from(TodoListEntity entity) {
    int done =
        (int) entity.getItems().stream().filter(i -> Boolean.TRUE.equals(i.getDone())).count();
    return TodoListSummaryResponse.builder()
        .id(entity.getId())
        .name(entity.getName())
        .deadline(entity.getDeadline())
        .itemCount(entity.getItems().size())
        .doneCount(done)
        .build();
  }
}
