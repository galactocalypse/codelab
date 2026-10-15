package com.codelab.todos.service;

import com.codelab.todos.model.AddTodoItemRequest;
import com.codelab.todos.model.CreateTodoListRequest;
import com.codelab.todos.model.CreateTodoListResponse;
import com.codelab.todos.model.GetTodoListResponse;
import com.codelab.todos.model.TodoListSummaryResponse;
import com.codelab.todos.model.UpdateTodoItemRequest;
import java.util.List;

/**
 * Application service for todo lists. All methods run against the per-module {@code
 * todos.transactionManager}.
 */
public interface TodoService {

  /** Creates and persists a todo list with an optional deadline. */
  CreateTodoListResponse createTodoList(CreateTodoListRequest request);

  /** Reads a todo list with all of its items. */
  GetTodoListResponse getTodoList(Long id);

  /** Lists every todo list, newest last, without hydrating items. */
  List<TodoListSummaryResponse> listTodoLists();

  /** Appends an item to the list. */
  GetTodoListResponse addTodoItem(Long listId, AddTodoItemRequest request);

  /** Partially updates an item (description, notes, done - any subset). */
  GetTodoListResponse updateTodoItem(Long listId, Long itemId, UpdateTodoItemRequest request);

  /** Marks an item done or undone. */
  GetTodoListResponse toggleTodoItem(Long listId, Long itemId, boolean done);
}
