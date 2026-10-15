package com.codelab.todos.repository;

import com.codelab.todos.entity.TodoListEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/** Per-module JPA repository for todo lists. */
public interface TodoListRepository extends JpaRepository<TodoListEntity, Long> {}
