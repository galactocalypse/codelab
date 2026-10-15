package com.codelab.todos.controller;

import com.codelab.todos.model.AddTodoItemRequest;
import com.codelab.todos.model.CreateTodoListRequest;
import com.codelab.todos.model.CreateTodoListResponse;
import com.codelab.todos.model.GetTodoListResponse;
import com.codelab.todos.model.TodoListSummaryResponse;
import com.codelab.todos.model.UpdateTodoItemRequest;
import com.codelab.todos.service.TodoService;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** REST controller exposing todo list and item operations. */
@RestController
@AllArgsConstructor
@RequestMapping("/api/todos")
public class TodoController {

  private final TodoService service;

  /** Creates a todo list. */
  @PostMapping
  public CreateTodoListResponse create(@RequestBody CreateTodoListRequest request) {
    return service.createTodoList(request);
  }

  /** Reads a todo list by id (includes items). */
  @GetMapping("/{id}")
  public GetTodoListResponse get(@PathVariable("id") Long id) {
    return service.getTodoList(id);
  }

  /** Lists all todo lists. */
  @GetMapping
  public List<TodoListSummaryResponse> list() {
    return service.listTodoLists();
  }

  /** Adds an item to a list. */
  @PostMapping("/{id}/items")
  public GetTodoListResponse addItem(
      @PathVariable("id") Long id, @RequestBody AddTodoItemRequest request) {
    return service.addTodoItem(id, request);
  }

  /** Updates an item in a list (partial: description, notes, done). */
  @PutMapping("/{id}/items/{itemId}")
  public GetTodoListResponse updateItem(
      @PathVariable("id") Long id,
      @PathVariable("itemId") Long itemId,
      @RequestBody UpdateTodoItemRequest request) {
    return service.updateTodoItem(id, itemId, request);
  }

  /** Marks the item done. */
  @PatchMapping("/{id}/items/{itemId}/done")
  public GetTodoListResponse markDone(
      @PathVariable("id") Long id, @PathVariable("itemId") Long itemId) {
    return service.toggleTodoItem(id, itemId, true);
  }

  /** Marks the item undone. */
  @PatchMapping("/{id}/items/{itemId}/undone")
  public GetTodoListResponse markUndone(
      @PathVariable("id") Long id, @PathVariable("itemId") Long itemId) {
    return service.toggleTodoItem(id, itemId, false);
  }
}
