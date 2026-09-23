package com.codelab.common.spring.eventbus;

import org.springframework.core.annotation.AliasFor;
import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CodelabTopic {

    /**
     * Logical topic name, e.g. "order.created".
     * Tenant + namespace are resolved separately from CodelabModule config
     * and combined with this at bean-registration time.
     */
    @AliasFor("name")
    String value() default "";

    @AliasFor("value")
    String name() default "";

}
