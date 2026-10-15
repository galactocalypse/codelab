package com.codelab.core.task;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Immutable registry of every {@link com.codelab.common.spring.task.CodelabTask} declared by the
 * discovered modules, keyed by task name. Built once by {@link CodelabTaskRegistryUtils} during
 * module registration; the dispatcher reads it to list and resolve tasks.
 */
public final class CodelabTaskCatalog {

  private final Map<String, CodelabTaskDescriptor> byName;

  public CodelabTaskCatalog(Map<String, CodelabTaskDescriptor> byName) {
    this.byName = Map.copyOf(byName);
  }

  /** All tasks, ordered by name for stable {@code list} output. */
  public List<CodelabTaskDescriptor> tasks() {
    return byName.values().stream()
        .sorted(Comparator.comparing(CodelabTaskDescriptor::name))
        .toList();
  }

  public Optional<CodelabTaskDescriptor> find(String name) {
    return Optional.ofNullable(byName.get(name));
  }

  public Set<String> names() {
    return byName.keySet();
  }
}
