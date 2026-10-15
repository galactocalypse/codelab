package com.codelab.todos.autoconfigure;

import com.codelab.common.spring.persistence.CodelabModule;
import com.codelab.common.spring.persistence.CodelabModuleProvider;
import java.util.Map;

public class TodosModuleProvider implements CodelabModuleProvider {
  @Override
  public CodelabModule provide() {
    return new CodelabModule("todos", "com.codelab.todos", "todosPU", "todos", Map.of());
  }
}
