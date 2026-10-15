package com.codelab.todos.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * A todo list: a named container with a single {@code deadline} shared by all of its items. Items
 * are owned children ({@code orphanRemoval}) so deleting a list deletes its items.
 */
@Data
@Builder
@Entity
@Table(name = "todo_lists")
@NoArgsConstructor
@AllArgsConstructor
public class TodoListEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String name;

  /** Deadline applying to every item in this list. */
  @Column(name = "deadline")
  private LocalDate deadline;

  @Builder.Default
  @OneToMany(mappedBy = "todoList", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("id ASC")
  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  private List<TodoItemEntity> items = new ArrayList<>();

  /** Appends a new undone item owned by this list. */
  public void addItem(String description, String notes) {
    items.add(
        TodoItemEntity.builder()
            .todoList(this)
            .description(description)
            .notes(notes)
            .done(false)
            .build());
  }

  /** Detaches an item so {@code orphanRemoval} deletes it on flush. */
  public void removeItem(Long itemId) {
    items.removeIf(
        item -> {
          boolean match = item.getId() != null && item.getId().equals(itemId);
          if (match) {
            item.setTodoList(null);
          }
          return match;
        });
  }
}
