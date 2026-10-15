package com.codelab.todos.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.codelab.todos.exception.TodoNotFoundException;
import com.codelab.todos.model.AddTodoItemRequest;
import com.codelab.todos.model.CreateTodoListRequest;
import com.codelab.todos.model.CreateTodoListResponse;
import com.codelab.todos.model.GetTodoListResponse;
import com.codelab.todos.model.UpdateTodoItemRequest;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Integration test for the todo service against real Postgres.
 *
 * <p>It exercises the whole module surface the REST layer delegates to — creating a list, adding
 * items, partial updates, and done/undone toggling — and checks that the schema Hibernate generates
 * from the entities is valid Postgres (deadline column, TEXT notes, the owned-items cascade).
 */
@Testcontainers(disabledWithoutDocker = true)
@DataJpaTest(showSql = false, properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class TodoServiceIntegrationTest {

  @Container
  static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>("postgres:18").withDatabaseName("todos");

  /**
   * The service resolves its transaction manager by the name the module registrar gives it in
   * production. The slice only knows Boot's default name, so it is aliased onto it — one bean, both
   * names.
   */
  @TestConfiguration
  @Import(TodoServiceImpl.class)
  static class Wiring {

    @Bean
    static BeanFactoryPostProcessor aliasTodosTransactionManager() {
      return beanFactory ->
          beanFactory.registerAlias("transactionManager", "todos.transactionManager");
    }
  }

  @DynamicPropertySource
  static void postgresProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
  }

  @Autowired TodoService service;

  @Test
  void creatingAListAddsItemsAndPersistsTheDeadline() {
    LocalDate deadline = LocalDate.of(2026, 11, 1);
    CreateTodoListResponse created =
        service.createTodoList(
            CreateTodoListRequest.builder().name("Release 1.0").deadline(deadline).build());

    service.addTodoItem(
        created.getId(), AddTodoItemRequest.builder().description("Write docs").build());
    GetTodoListResponse list =
        service.addTodoItem(
            created.getId(),
            AddTodoItemRequest.builder().description("Ship it").notes("after QA").build());

    assertThat(list.getName()).isEqualTo("Release 1.0");
    assertThat(list.getDeadline()).isEqualTo(deadline);
    assertThat(list.getItems()).hasSize(2);
    assertThat(list.getItems())
        .extracting(item -> item.getDescription())
        .containsExactly("Write docs", "Ship it");
    assertThat(list.getItems()).allSatisfy(item -> assertThat(item.getDone()).isFalse());
    assertThat(list.getItems())
        .filteredOn(item -> item.getDescription().equals("Ship it"))
        .singleElement()
        .satisfies(item -> assertThat(item.getNotes()).isEqualTo("after QA"));
  }

  @Test
  void itemsCanBePartiallyUpdatedAndToggled() {
    Long listId =
        service.createTodoList(CreateTodoListRequest.builder().name("Chores").build()).getId();
    Long itemId =
        service
            .addTodoItem(listId, AddTodoItemRequest.builder().description("Dishes").build())
            .getItems()
            .get(0)
            .getId();

    // Notes only: description and done must be untouched.
    GetTodoListResponse afterNotes =
        service.updateTodoItem(
            listId, itemId, UpdateTodoItemRequest.builder().notes("use the good soap").build());
    assertThat(afterNotes.getItems().get(0).getNotes()).isEqualTo("use the good soap");
    assertThat(afterNotes.getItems().get(0).getDescription()).isEqualTo("Dishes");
    assertThat(afterNotes.getItems().get(0).getDone()).isFalse();

    assertThat(service.toggleTodoItem(listId, itemId, true).getItems().get(0).getDone()).isTrue();
    assertThat(service.toggleTodoItem(listId, itemId, false).getItems().get(0).getDone()).isFalse();

    // Description + done in one update.
    GetTodoListResponse afterFullUpdate =
        service.updateTodoItem(
            listId,
            itemId,
            UpdateTodoItemRequest.builder().description("Wash dishes").done(true).build());
    assertThat(afterFullUpdate.getItems().get(0).getDescription()).isEqualTo("Wash dishes");
    assertThat(afterFullUpdate.getItems().get(0).getDone()).isTrue();
    assertThat(afterFullUpdate.getItems().get(0).getNotes()).isEqualTo("use the good soap");
  }

  @Test
  void listsSummariseItemsAndCompletion() {
    Long listId =
        service
            .createTodoList(
                CreateTodoListRequest.builder()
                    .name("Groceries")
                    .deadline(LocalDate.of(2026, 12, 24))
                    .build())
            .getId();
    Long milk =
        service
            .addTodoItem(listId, AddTodoItemRequest.builder().description("Milk").build())
            .getItems()
            .get(0)
            .getId();
    service.addTodoItem(listId, AddTodoItemRequest.builder().description("Bread").build());
    service.toggleTodoItem(listId, milk, true);

    assertThat(service.listTodoLists())
        .singleElement()
        .satisfies(
            summary -> {
              assertThat(summary.getName()).isEqualTo("Groceries");
              assertThat(summary.getDeadline()).isEqualTo(LocalDate.of(2026, 12, 24));
              assertThat(summary.getItemCount()).isEqualTo(2);
              assertThat(summary.getDoneCount()).isEqualTo(1);
            });
  }

  @Test
  void unknownListOrItemIsRejected() {
    assertThatThrownBy(() -> service.getTodoList(999L))
        .isInstanceOf(TodoNotFoundException.class)
        .hasMessageContaining("999");

    Long listId =
        service.createTodoList(CreateTodoListRequest.builder().name("Empty").build()).getId();
    assertThatThrownBy(
            () ->
                service.updateTodoItem(
                    listId, 999L, UpdateTodoItemRequest.builder().description("nope").build()))
        .isInstanceOf(TodoNotFoundException.class)
        .hasMessageContaining("999");
  }
}
