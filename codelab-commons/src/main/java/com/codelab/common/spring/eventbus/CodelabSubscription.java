package com.codelab.common.spring.eventbus;

import java.lang.annotation.*;
import org.apache.pulsar.client.api.SubscriptionInitialPosition;
import org.apache.pulsar.client.api.SubscriptionType;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CodelabSubscription {

  /** Same topic naming convention as @CodelabTopic. */
  String topic();

  /**
   * Subscription name. This is the identity Pulsar uses to track cursor position — two listeners
   * with the same topic+subscription are cooperating consumers, not independent ones. Make this
   * explicit rather than derived, so nobody renames a class and silently resets (or splits) a
   * consumer group's cursor.
   */
  String subscriptionName();

  SubscriptionType type() default SubscriptionType.Key_Shared;

  SubscriptionInitialPosition initialPosition() default SubscriptionInitialPosition.Latest;

  int concurrency() default 1;

  /**
   * Name of a bean implementing your dead-letter/retry policy, or "" for module default. Keep this
   * a lookup key, not inline config — retry policy tends to get reused across many listeners.
   */
  String deadLetterPolicyRef() default "";

  int ackTimeoutSeconds() default 60;
}
