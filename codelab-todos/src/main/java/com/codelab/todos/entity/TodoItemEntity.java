package com.codelab.todos.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * A single todo item belonging to a {@link TodoListEntity}. Carries its own {@code notes} for
 * ad-hoc user notes and a {@code done} flag toggled via the REST API.
 */
@Data
@Builder
@Entity
@Table(name = "todo_items")
@NoArgsConstructor
@AllArgsConstructor
public class TodoItemEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "todo_list_id", nullable = false)
  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  private TodoListEntity todoList;

  @Column(nullable = false)
  private String description;

  /** Free-form ad-hoc notes the user may add or update at any time. */
  @Column(columnDefinition = "TEXT")
  private String notes;

  @Column(nullable = false)
  private Boolean done;

  @Transient
  public Long getTodoListId() {
    return todoList != null ? todoList.getId() : null;
  }
}
