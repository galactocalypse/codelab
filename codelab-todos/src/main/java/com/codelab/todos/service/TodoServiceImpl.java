package com.codelab.todos.service;

import com.codelab.todos.entity.TodoItemEntity;
import com.codelab.todos.entity.TodoListEntity;
import com.codelab.todos.exception.TodoNotFoundException;
import com.codelab.todos.model.AddTodoItemRequest;
import com.codelab.todos.model.CreateTodoListRequest;
import com.codelab.todos.model.CreateTodoListResponse;
import com.codelab.todos.model.GetTodoListResponse;
import com.codelab.todos.model.TodoListSummaryResponse;
import com.codelab.todos.model.UpdateTodoItemRequest;
import com.codelab.todos.repository.TodoListRepository;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

/** Default implementation of {@link TodoService}. */
@AllArgsConstructor
public class TodoServiceImpl implements TodoService {

  private final TodoListRepository todoListRepository;

  @Override
  @Transactional(transactionManager = "todos.transactionManager")
  public CreateTodoListResponse createTodoList(CreateTodoListRequest request) {
    TodoListEntity saved =
        todoListRepository.save(
            TodoListEntity.builder()
                .name(request.getName())
                .deadline(request.getDeadline())
                .build());
    return CreateTodoListResponse.from(saved);
  }

  @Override
  @Transactional(transactionManager = "todos.transactionManager", readOnly = true)
  public GetTodoListResponse getTodoList(Long id) {
    TodoListEntity list =
        todoListRepository
            .findById(id)
            .orElseThrow(() -> new TodoNotFoundException("Todo list " + id + " not found"));
    return GetTodoListResponse.from(list);
  }

  @Override
  @Transactional(transactionManager = "todos.transactionManager", readOnly = true)
  public List<TodoListSummaryResponse> listTodoLists() {
    return todoListRepository.findAll().stream()
        .map(TodoListSummaryResponse::from)
        .collect(Collectors.toList());
  }

  @Override
  @Transactional(transactionManager = "todos.transactionManager")
  public GetTodoListResponse addTodoItem(Long listId, AddTodoItemRequest request) {
    TodoListEntity list =
        todoListRepository
            .findById(listId)
            .orElseThrow(() -> new TodoNotFoundException("Todo list " + listId + " not found"));
    list.addItem(request.getDescription(), request.getNotes());
    todoListRepository.save(list);
    return GetTodoListResponse.from(list);
  }

  @Override
  @Transactional(transactionManager = "todos.transactionManager")
  public GetTodoListResponse updateTodoItem(
      Long listId, Long itemId, UpdateTodoItemRequest request) {
    TodoListEntity list =
        todoListRepository
            .findById(listId)
            .orElseThrow(() -> new TodoNotFoundException("Todo list " + listId + " not found"));

    TodoItemEntity item =
        list.getItems().stream()
            .filter(i -> i.getId() != null && i.getId().equals(itemId))
            .findFirst()
            .orElseThrow(() -> new TodoNotFoundException("Todo item " + itemId + " not found"));

    if (request.getDescription() != null) {
      item.setDescription(request.getDescription());
    }
    if (request.getNotes() != null) {
      item.setNotes(request.getNotes());
    }
    if (request.getDone() != null) {
      item.setDone(request.getDone());
    }

    todoListRepository.save(list);
    return GetTodoListResponse.from(list);
  }

  @Override
  @Transactional(transactionManager = "todos.transactionManager")
  public GetTodoListResponse toggleTodoItem(Long listId, Long itemId, boolean done) {
    return updateTodoItem(listId, itemId, UpdateTodoItemRequest.builder().done(done).build());
  }
}
