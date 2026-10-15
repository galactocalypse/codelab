package com.codelab.core.task;

import com.codelab.common.spring.task.CodelabTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.boot.CommandLineRunner;

/**
 * Single dispatcher for every module-contributed {@link CodelabTask}. A normal boot selects nothing
 * and runs nothing; {@code --codelab.task=<name>} runs exactly one task; {@code
 * --codelab.task=list} prints the catalog. Selection is a single namespaced handle rather than one
 * boolean per job, and because it runs at most one task there is no multi-runner ordering hazard.
 *
 * <p>The selected task is resolved lazily from the bean factory, so heavy tasks are only
 * instantiated when chosen. Selected tasks run headless: {@code MegalithApplication} starts a
 * non-web context and exits with the task's code.
 */
@Slf4j
public class CodelabTaskRunner implements CommandLineRunner {

  /** Property/arg that selects a task, e.g. {@code --codelab.task=movies.feed}. */
  public static final String SELECTOR = "codelab.task";

  /** Synthetic task name that lists the catalog instead of running anything. */
  public static final String LIST = "list";

  private static final int EXIT_FAILURE = 1;
  private static final int EXIT_USAGE = 2;

  private final CodelabTaskCatalog catalog;
  private final ListableBeanFactory beanFactory;

  private int exitCode;

  public CodelabTaskRunner(CodelabTaskCatalog catalog, ListableBeanFactory beanFactory) {
    this.catalog = catalog;
    this.beanFactory = beanFactory;
  }

  /** Whether the raw main() args select a task, so {@code main} can decide to boot headless. */
  public static boolean isRequested(String[] args) {
    return selectedFrom(args) != null;
  }

  static String selectedFrom(String[] args) {
    if (args == null) {
      return null;
    }
    String prefix = "--" + SELECTOR + "=";
    for (String arg : args) {
      if (arg != null && arg.startsWith(prefix)) {
        String value = arg.substring(prefix.length()).trim();
        return value.isEmpty() ? null : value;
      }
    }
    return null;
  }

  /**
   * Result of {@link #run(String...)}; read by {@code MegalithApplication.main} for the exit code.
   */
  public int getExitCode() {
    return exitCode;
  }

  @Override
  public void run(String... args) {
    String selected = selectedFrom(args);
    if (selected == null) {
      return; // normal boot
    }

    if (LIST.equals(selected)) {
      printCatalog();
      return;
    }

    CodelabTaskDescriptor descriptor = catalog.find(selected).orElse(null);
    if (descriptor == null) {
      log.error("Unknown task '{}'. Available tasks: {}", selected, catalog.names());
      exitCode = EXIT_USAGE;
      return;
    }

    log.info("Running task '{}' ({})", descriptor.name(), descriptor.module());
    try {
      exitCode = beanFactory.getBean(descriptor.beanName(), CodelabTask.class).run();
      log.info("Task '{}' finished with exit code {}", descriptor.name(), exitCode);
    } catch (Exception e) {
      log.error("Task '{}' failed", descriptor.name(), e);
      exitCode = EXIT_FAILURE;
    }
  }

  private void printCatalog() {
    if (catalog.tasks().isEmpty()) {
      System.out.println("No codelab tasks registered.");
      return;
    }
    System.out.println("Available codelab tasks (select one with --" + SELECTOR + "=<name>):");
    for (CodelabTaskDescriptor descriptor : catalog.tasks()) {
      String description =
          descriptor.description().isBlank() ? "" : " — " + descriptor.description();
      System.out.printf("  %-24s [%s]%s%n", descriptor.name(), descriptor.module(), description);
    }
  }
}
