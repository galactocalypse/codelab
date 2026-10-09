package com.codelab.core.eventbus;

import com.codelab.common.spring.eventbus.CodelabSubscription;
import com.codelab.common.spring.eventbus.CodelabTopic;
import com.codelab.common.spring.jobbus.CodelabJobSubscription;
import com.codelab.common.spring.jobbus.CodelabJobTopic;
import com.codelab.common.spring.persistence.CodelabModule;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

public final class CodelabTopicResolver {

  /**
   * The two topic kinds sharing one Pulsar stack. Every physical topic carries its kind as a name
   * prefix ({@code event.*} / {@code job.*} by default), so an event and a job can never claim the
   * same topic within a module and ops can tell the kind apart from any topic name.
   */
  public enum TopicKind {
    EVENT("app.pulsar.events.topic-prefix", "event"),
    JOB("app.pulsar.jobs.topic-prefix", "job");

    private final String prefixProperty;
    private final String defaultPrefix;

    TopicKind(String prefixProperty, String defaultPrefix) {
      this.prefixProperty = prefixProperty;
      this.defaultPrefix = defaultPrefix;
    }

    public String prefixProperty() {
      return prefixProperty;
    }

    public String defaultPrefix() {
      return defaultPrefix;
    }
  }

  private CodelabTopicResolver() {}

  public static String resolveTenant(Environment environment, String moduleName) {
    return environment.getRequiredProperty("app.pulsar.tenant.prefix") + moduleName;
  }

  public static String resolveTopicName(
      CodelabModule module, CodelabSubscription annotation, Environment environment) {
    return resolveTopicName(module.name(), annotation, environment);
  }

  public static String resolveTopicName(
      String moduleName, CodelabSubscription annotation, Environment environment) {
    if (!StringUtils.hasText(annotation.topic())) {
      throw new IllegalStateException(
          "@CodelabSubscription on a consumer interface must specify a non-blank topic name");
    }
    return resolveTopicName(moduleName, TopicKind.EVENT, annotation.topic(), environment);
  }

  public static String resolveTopicName(
      String moduleName, CodelabJobSubscription annotation, Environment environment) {
    if (!StringUtils.hasText(annotation.topic())) {
      throw new IllegalStateException(
          "@CodelabJobSubscription on a job consumer must specify a non-blank topic name");
    }
    return resolveTopicName(moduleName, TopicKind.JOB, annotation.topic(), environment);
  }

  /**
   * Composes the fully-qualified Pulsar topic: persistent://tenant/namespace/<kind>.<logical>
   *
   * <p>Tenant comes from the module (each module IS a tenant, per your original design). Namespace
   * comes from megalith-level config (roughly your env: dev/staging/prod), unless the annotation
   * explicitly overrides it — that override should be rare and probably logged, since it's an
   * escape hatch from the module's default isolation.
   */
  public static String resolveTopicName(
      CodelabModule module, CodelabTopic annotation, Environment environment) {
    return resolveTopicName(module.name(), annotation, environment);
  }

  public static String resolveTopicName(
      String moduleName, CodelabTopic annotation, Environment environment) {
    return resolveTopicName(
        moduleName,
        TopicKind.EVENT,
        logicalTopic(annotation.value(), annotation.name(), "@CodelabTopic"),
        environment);
  }

  public static String resolveTopicName(
      String moduleName, CodelabJobTopic annotation, Environment environment) {
    return resolveTopicName(
        moduleName,
        TopicKind.JOB,
        logicalTopic(annotation.value(), annotation.name(), "@CodelabJobTopic"),
        environment);
  }

  /**
   * Kind-aware core used by the registration paths that read the logical topic off a merged
   * annotation generically: applies the kind's topic prefix, then composes
   * persistent://tenant/namespace/topic.
   */
  public static String resolveTopicName(
      String moduleName, TopicKind kind, String logicalTopic, Environment environment) {
    if (!StringUtils.hasText(logicalTopic)) {
      throw new IllegalStateException(
          "Topic name must be non-blank for tenant '"
              + resolveTenant(environment, moduleName)
              + "' — set the topic name on the publisher/consumer annotation");
    }
    return compose(moduleName, withKindPrefix(kind, logicalTopic, environment), environment);
  }

  /**
   * Prefixes the logical topic with the kind prefix ({@code app.pulsar.events.topic-prefix} /
   * {@code app.pulsar.jobs.topic-prefix}, defaults {@code event} / {@code job}). A blank configured
   * value disables prefixing — the escape hatch for topics produced outside this framework.
   */
  private static String withKindPrefix(TopicKind kind, String topic, Environment environment) {
    String raw = environment.getProperty(kind.prefixProperty(), kind.defaultPrefix());
    String prefix = raw == null ? "" : raw.trim();
    if (!StringUtils.hasText(prefix)) {
      return topic;
    }
    return prefix + "." + topic;
  }

  private static String compose(String moduleName, String topic, Environment environment) {
    String tenant = resolveTenant(environment, moduleName);
    String namespace = environment.getRequiredProperty("app.pulsar.namespace");
    if (!StringUtils.hasText(tenant)) {
      throw new IllegalStateException(
          "CodelabModule '" + moduleName + "' has no tenant configured");
    }

    if (!StringUtils.hasText(namespace)) {
      throw new IllegalStateException("No namespace resolved for tenant '" + tenant + "'");
    }

    return String.format("persistent://%s/%s/%s", tenant, namespace, topic);
  }

  private static String logicalTopic(String value, String name, String annotationLabel) {
    if (StringUtils.hasText(value)) {
      return value;
    }
    if (StringUtils.hasText(name)) {
      return name;
    }
    throw new IllegalStateException(annotationLabel + " must specify a non-blank topic name");
  }
}
