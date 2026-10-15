package com.codelab.core.task;

import com.codelab.common.spring.persistence.CodelabModule;
import com.codelab.common.spring.task.CodelabTask;
import com.codelab.common.spring.task.CodelabTaskSpec;
import com.codelab.core.CodelabBeanNaming;
import com.codelab.core.CodelabModuleRegistrationContext;
import com.codelab.core.utils.CodelabPackageScanner;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;

/**
 * Discovers and registers {@link CodelabTask}s declared by a module, mirroring how consumers are
 * registered in {@code CodelabPulsarRegistryUtils}: scan the module base package for
 * implementations of the contract, validate the annotation, and register each as a
 * lazily-initialised singleton bean (so a normal web boot never instantiates a task).
 */
@Slf4j
public final class CodelabTaskRegistryUtils {

  /** Bean name of the {@link CodelabTaskCatalog} registered by {@link #registerCatalog}. */
  public static final String CODELAB_TASK_CATALOG = "codelabTaskCatalog";

  public static final String MODULE_ATTRIBUTE = "codelab.task.module";

  private CodelabTaskRegistryUtils() {}

  /**
   * Registers every {@link CodelabTask} under {@code module.basePackage()} and returns descriptors
   * for the catalog.
   */
  public static List<CodelabTaskDescriptor> registerTasks(
      CodelabModule module, CodelabModuleRegistrationContext context) {
    if (module.basePackage() == null || module.basePackage().isBlank()) {
      return List.of();
    }

    Set<Class<?>> candidates =
        CodelabPackageScanner.scan(module.basePackage(), CodelabTask.class, false, true);
    List<CodelabTaskDescriptor> descriptors = new ArrayList<>();
    for (Class<?> candidate : candidates) {
      CodelabTaskSpec spec =
          CodelabPackageScanner.validateAndGetAnnotation(candidate, CodelabTaskSpec.class);
      String taskName = spec.value().trim();
      if (taskName.isEmpty()) {
        throw new IllegalStateException(
            "@"
                + CodelabTaskSpec.class.getSimpleName()
                + " on "
                + candidate.getName()
                + " must declare a non-blank name");
      }

      BeanDefinition definition =
          BeanDefinitionBuilder.genericBeanDefinition(candidate)
              .setScope(BeanDefinition.SCOPE_SINGLETON)
              // lazy: tasks exist as definitions only; the dispatcher instantiates just the
              // selected one
              .setLazyInit(true)
              .getBeanDefinition();
      definition.setAttribute(MODULE_ATTRIBUTE, module.name());

      String beanName = CodelabBeanNaming.deriveBeanName(module, candidate.getSimpleName());
      context.getRegistry().registerBeanDefinition(beanName, definition);
      descriptors.add(
          new CodelabTaskDescriptor(taskName, spec.description(), beanName, module.name()));
      log.debug(
          "Registered task '{}' from module {} as bean {}", taskName, module.name(), beanName);
    }
    return descriptors;
  }

  /**
   * Registers the {@link CodelabTaskCatalog} singleton from all modules' descriptors, failing fast
   * on a duplicate task name. Always registered (possibly empty) so the dispatcher can depend on
   * it.
   */
  public static void registerCatalog(
      List<CodelabTaskDescriptor> descriptors, CodelabModuleRegistrationContext context) {
    Map<String, CodelabTaskDescriptor> byName = new LinkedHashMap<>();
    for (CodelabTaskDescriptor descriptor : descriptors) {
      CodelabTaskDescriptor previous = byName.putIfAbsent(descriptor.name(), descriptor);
      if (previous != null) {
        throw new IllegalStateException(
            "Duplicate task name '"
                + descriptor.name()
                + "' registered by modules '"
                + previous.module()
                + "' and '"
                + descriptor.module()
                + "'");
      }
    }

    BeanDefinition definition =
        BeanDefinitionBuilder.genericBeanDefinition(CodelabTaskCatalog.class)
            .addConstructorArgValue(byName)
            .getBeanDefinition();
    context.getRegistry().registerBeanDefinition(CODELAB_TASK_CATALOG, definition);
  }
}
