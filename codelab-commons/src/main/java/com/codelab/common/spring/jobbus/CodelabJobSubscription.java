package com.codelab.common.spring.jobbus;

import java.lang.annotation.*;
import org.apache.pulsar.client.api.SubscriptionInitialPosition;
import org.apache.pulsar.client.api.SubscriptionType;

/**
 * Subscription declaration for a {@link CodelabJobConsumer}. The job-path counterpart of {@link
 * com.codelab.common.spring.eventbus.CodelabSubscription}: the framework registers the listener
 * with a plain decode-and-delegate dispatcher (no business-version gate) and applies the {@link
 * #deadLetterPolicyRef()} policy for native retry/DLQ.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CodelabJobSubscription {

  /** Same topic naming convention as @CodelabJobTopic. */
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
   * Name of a bean implementing the dead-letter/retry policy, or "" for no DLQ — a failing message
   * would then be redelivered indefinitely. Job consumers that process real work should point at a
   * policy; keep it a lookup key, not inline config.
   */
  String deadLetterPolicyRef() default "";

  int ackTimeoutSeconds() default 60;
}
