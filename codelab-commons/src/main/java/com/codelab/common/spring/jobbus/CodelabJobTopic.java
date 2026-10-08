package com.codelab.common.spring.jobbus;

import java.lang.annotation.*;
import org.springframework.core.annotation.AliasFor;

/**
 * Topic declaration for a {@link CodelabJobPublisher} interface. The job-path counterpart of {@link
 * com.codelab.common.spring.eventbus.CodelabTopic} — same naming convention, different workflow.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CodelabJobTopic {

  /**
   * Logical topic name, e.g. "pending-movies". Tenant + namespace are resolved separately from
   * CodelabModule config and combined with this at bean-registration time.
   */
  @AliasFor("name")
  String value() default "";

  @AliasFor("value")
  String name() default "";
}
