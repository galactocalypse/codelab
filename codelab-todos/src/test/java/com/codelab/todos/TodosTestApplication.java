package com.codelab.todos;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;

/**
 * Test-only entry point. The module is a library, so it has no application class of its own; slice
 * tests need a {@code @SpringBootConfiguration} above {@code com.codelab.todos.entity} / {@code
 * .repository} to locate entities and repositories from.
 */
@SpringBootConfiguration
@EnableAutoConfiguration
public class TodosTestApplication {}
