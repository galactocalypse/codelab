package com.codelab.core.task;

/**
 * Metadata for a discovered {@link com.codelab.common.spring.task.CodelabTask}. Carried in the
 * {@link CodelabTaskCatalog} so the dispatcher can list and locate tasks without instantiating
 * them.
 *
 * @param name the {@code @CodelabTaskSpec} name, e.g. {@code movies.feed}
 * @param description one-line summary for {@code --codelab.task=list}
 * @param beanName the Spring bean name the task was registered under; used to resolve it lazily
 * @param module the owning module name
 */
public record CodelabTaskDescriptor(
    String name, String description, String beanName, String module) {}
