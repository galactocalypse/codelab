package com.codelab.common.spring.task;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a {@link CodelabTask} implementation and declares the name the dispatcher selects it by
 * ({@code --codelab.task=<name>}).
 *
 * <p>The registrar discovers annotated tasks by scanning the module base package, so an
 * implementation only needs this annotation — no {@code @Component} and no autoconfiguration
 * {@code @Bean} method.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CodelabTaskSpec {

  /**
   * Globally unique task name, conventionally {@code <module>.<verb>} (e.g. {@code movies.feed}).
   * Registration fails fast if two tasks across all modules claim the same name.
   */
  String value();

  /** One-line human-readable summary shown by {@code --codelab.task=list}. */
  String description() default "";
}
