package com.codelab.core.eventbus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.codelab.common.spring.eventbus.CodelabSubscription;
import com.codelab.common.spring.eventbus.CodelabTopic;
import com.codelab.common.spring.jobbus.CodelabJobSubscription;
import com.codelab.common.spring.jobbus.CodelabJobTopic;
import com.codelab.core.eventbus.CodelabTopicResolver.TopicKind;
import org.junit.jupiter.api.Test;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.mock.env.MockEnvironment;

class CodelabTopicResolverTest {

  private final MockEnvironment environment =
      new MockEnvironment()
          .withProperty("app.pulsar.tenant.prefix", "codelab-")
          .withProperty("app.pulsar.namespace", "local");

  @CodelabTopic("created-orders")
  interface TestEventPublisher {}

  @CodelabJobTopic("pending-movies")
  interface TestJobPublisher {}

  @CodelabSubscription(topic = "created-orders", subscriptionName = "test-sub")
  static class TestEventConsumer {}

  @CodelabJobSubscription(topic = "orders-cdc.public.orders", subscriptionName = "test-sub")
  static class TestJobConsumer {}

  @Test
  void eventTopicsGetTheEventKindPrefix() {
    CodelabTopic annotation =
        AnnotatedElementUtils.findMergedAnnotation(TestEventPublisher.class, CodelabTopic.class);

    assertThat(CodelabTopicResolver.resolveTopicName("orders", annotation, environment))
        .isEqualTo("persistent://codelab-orders/local/event.created-orders");

    assertThat(
            CodelabTopicResolver.resolveTopicName(
                "orders", TopicKind.EVENT, "created-orders", environment))
        .isEqualTo("persistent://codelab-orders/local/event.created-orders");
  }

  @Test
  void jobTopicsGetTheJobKindPrefix() {
    CodelabJobTopic annotation =
        AnnotatedElementUtils.findMergedAnnotation(TestJobPublisher.class, CodelabJobTopic.class);

    assertThat(CodelabTopicResolver.resolveTopicName("movies", annotation, environment))
        .isEqualTo("persistent://codelab-movies/local/job.pending-movies");
  }

  @Test
  void eventSubscriptionsResolveWithTheEventPrefixOnTheConsumeEnd() {
    CodelabSubscription annotation =
        AnnotatedElementUtils.findMergedAnnotation(
            TestEventConsumer.class, CodelabSubscription.class);

    assertThat(CodelabTopicResolver.resolveTopicName("orders", annotation, environment))
        .isEqualTo("persistent://codelab-orders/local/event.created-orders");
  }

  @Test
  void jobSubscriptionsResolveWithTheJobPrefixAndPreserveDottedLogicalNames() {
    CodelabJobSubscription annotation =
        AnnotatedElementUtils.findMergedAnnotation(
            TestJobConsumer.class, CodelabJobSubscription.class);

    // Debezium writes <topic.prefix>.<schema>.<table> — the prefix is added by the framework
    // on the consume side and by topic.prefix on the produce side; the dots must survive.
    assertThat(CodelabTopicResolver.resolveTopicName("orders", annotation, environment))
        .isEqualTo("persistent://codelab-orders/local/job.orders-cdc.public.orders");
  }

  @Test
  void configuredPrefixOverridesTheDefault() {
    environment.setProperty("app.pulsar.jobs.topic-prefix", "work");

    assertThat(
            CodelabTopicResolver.resolveTopicName(
                "movies", TopicKind.JOB, "pending-movies", environment))
        .isEqualTo("persistent://codelab-movies/local/work.pending-movies");
  }

  @Test
  void blankPrefixDisablesKindPrefixing() {
    environment.setProperty("app.pulsar.events.topic-prefix", " ");

    assertThat(
            CodelabTopicResolver.resolveTopicName(
                "orders", TopicKind.EVENT, "created-orders", environment))
        .isEqualTo("persistent://codelab-orders/local/created-orders");
  }

  @Test
  void blankLogicalTopicIsRejected() {
    assertThatThrownBy(
            () ->
                CodelabTopicResolver.resolveTopicName("orders", TopicKind.EVENT, " ", environment))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("non-blank");
  }

  @Test
  void missingTenantPrefixFailsFast() {
    MockEnvironment bare = new MockEnvironment().withProperty("app.pulsar.namespace", "local");

    assertThatThrownBy(
            () ->
                CodelabTopicResolver.resolveTopicName(
                    "orders", TopicKind.EVENT, "created-orders", bare))
        .isInstanceOf(IllegalStateException.class);
  }
}
