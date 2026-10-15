package com.codelab.todos.autoconfigure;

import com.codelab.todos.controller.TodoController;
import com.codelab.todos.service.TodoServiceImpl;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@Import({TodoController.class, TodoServiceImpl.class})
public class TodosAutoConfiguration {}
