package com.codelab.common.spring.eventbus;

import java.lang.annotation.*;
import org.springframework.core.annotation.AliasFor;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CodelabTopic {

  /**
   * Logical topic name, e.g. "order.created". Tenant + namespace are resolved separately from
   * CodelabModule config and combined with this at bean-registration time.
   */
  @AliasFor("name")
  String value() default "";

  @AliasFor("value")
  String name() default "";
}
