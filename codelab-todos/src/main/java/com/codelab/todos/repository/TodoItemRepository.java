package com.codelab.todos.repository;

import com.codelab.todos.entity.TodoItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/** Per-module JPA repository for todo items. */
public interface TodoItemRepository extends JpaRepository<TodoItemEntity, Long> {}
