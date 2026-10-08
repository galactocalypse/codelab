package com.codelab.core.eventbus;

import com.codelab.common.spring.eventbus.BusinessVersion;
import com.codelab.common.spring.eventbus.CodelabEventConsumer;
import com.codelab.common.spring.eventbus.CodelabSubscription;
import com.codelab.common.spring.jobbus.CodelabJobConsumer;
import com.codelab.common.spring.jobbus.CodelabJobSubscription;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.pulsar.client.api.DeadLetterPolicy;
import org.apache.pulsar.client.api.Message;
import org.apache.pulsar.client.api.SubscriptionInitialPosition;
import org.apache.pulsar.client.api.SubscriptionType;
import org.apache.pulsar.common.schema.SchemaType;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.messaging.handler.annotation.support.MessageHandlerMethodFactory;
import org.springframework.pulsar.annotation.PulsarListenerConfigurer;
import org.springframework.pulsar.config.MethodPulsarListenerEndpoint;
import org.springframework.pulsar.config.PulsarListenerEndpoint;
import org.springframework.pulsar.config.PulsarListenerEndpointRegistrar;
import org.springframework.pulsar.listener.AckMode;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * Registers a Pulsar listener endpoint for every Codelab consumer bean:
 *
 * <ul>
 *   <li>{@link CodelabEventConsumer} → {@link CodelabVersionAwareMessageDispatcher}: business
 *       version gate before deserialization, framework DLQ routing for unsupported versions;
 *   <li>{@link CodelabJobConsumer} → {@link CodelabJobMessageDispatcher}: plain
 *       decode-and-delegate, failures handled by Pulsar-native retry + the subscription's {@link
 *       DeadLetterPolicy}.
 * </ul>
 */
@Slf4j
@RequiredArgsConstructor
public class CodelabPulsarListenerConfigurer
    implements PulsarListenerConfigurer, ResourceLoaderAware, EnvironmentAware {

  /** The framework listener method: version gate + payload decode + DLQ routing. */
  private static final Method DISPATCH_METHOD =
      ReflectionUtils.findMethod(
          CodelabVersionAwareMessageDispatcher.class, "dispatch", Message.class);

  /** The job-path listener method: payload decode + delegate, no version gate. */
  private static final Method JOB_DISPATCH_METHOD =
      ReflectionUtils.findMethod(CodelabJobMessageDispatcher.class, "dispatch", Message.class);

  private final ConfigurableListableBeanFactory beanFactory;
  private final MessageHandlerMethodFactory messageHandlerMethodFactory;
  private final CodelabDlqRouter codelabDlqRouter;

  @Setter private Environment environment;
  @Setter private ResourceLoader resourceLoader;

  @Override
  public void configurePulsarListeners(PulsarListenerEndpointRegistrar registrar) {
    beanFactory
        .getBeansOfType(CodelabEventConsumer.class)
        .forEach(
            (beanName, consumerBean) ->
                registrar.registerEndpoint(buildEventEndpoint(beanName, consumerBean), null));
    beanFactory
        .getBeansOfType(CodelabJobConsumer.class)
        .forEach(
            (beanName, consumerBean) ->
                registrar.registerEndpoint(buildJobEndpoint(beanName, consumerBean), null));
    // passing null for the factory means "use the default pulsarListenerContainerFactory",
    // same as an @PulsarListener with no containerFactory() set.
  }

  @SuppressWarnings("unchecked")
  private <V> PulsarListenerEndpoint buildEventEndpoint(
      String beanName, CodelabEventConsumer<?> consumerBean) {
    Class<?> targetClass = AopProxyUtils.ultimateTargetClass(consumerBean); // unwraps any proxy
    CodelabSubscription subscription =
        AnnotatedElementUtils.findMergedAnnotation(targetClass, CodelabSubscription.class);
    Assert.state(
        subscription != null,
        () ->
            "Bean '%s' implements CodelabEventConsumer but has no @CodelabSubscription"
                .formatted(beanName));

    ResolvedConsumer<V> resolved = resolve(targetClass, beanName, CodelabEventConsumer.class);

    // The version contract is enforced at startup: a consumer that can't handle *any* version —
    // or can't state its payload type — is a wiring bug, not a runtime surprise.
    Set<BusinessVersion> supportedVersions = consumerBean.getSupportedBusinessVersions();
    Assert.notEmpty(
        supportedVersions,
        () ->
            "Consumer '%s' declares no supported business versions"
                .formatted(targetClass.getSimpleName()));

    DeadLetterPolicy deadLetterPolicy =
        resolveDeadLetterPolicy(subscription.deadLetterPolicyRef(), targetClass);
    String dlqTopic =
        deadLetterPolicy != null && StringUtils.hasText(deadLetterPolicy.getDeadLetterTopic())
            ? deadLetterPolicy.getDeadLetterTopic()
            // mirrors Pulsar's own convention (RetryMessageUtil.getDLQTopic): topic + "-" + sub +
            // "-DLQ"
            : resolved.topic + "-" + resolved.qualifiedSubscriptionName + "-DLQ";

    CodelabVersionAwareMessageDispatcher<V> dispatcher =
        new CodelabVersionAwareMessageDispatcher<>(
            (CodelabEventConsumer<V>) consumerBean,
            resolved.payloadType,
            supportedVersions,
            dlqTopic,
            resolved.qualifiedSubscriptionName,
            codelabDlqRouter);

    MethodPulsarListenerEndpoint<byte[]> endpoint =
        buildEndpoint(
            beanName + "-codelabListener",
            dispatcher,
            DISPATCH_METHOD,
            subscription.type(),
            subscription.concurrency(),
            subscription.initialPosition(),
            subscription.ackTimeoutSeconds(),
            deadLetterPolicy);
    endpoint.setTopics(resolved.topic);
    endpoint.setSubscriptionName(resolved.qualifiedSubscriptionName);

    log.debug(
        "Registered listener {} for {} on {} (payload {}, versions {}, DLQ {})",
        beanName,
        targetClass.getSimpleName(),
        resolved.topic,
        resolved.payloadType.getSimpleName(),
        supportedVersions,
        dlqTopic);

    return endpoint;
  }

  @SuppressWarnings("unchecked")
  private <V> PulsarListenerEndpoint buildJobEndpoint(
      String beanName, CodelabJobConsumer<?> consumerBean) {
    Class<?> targetClass = AopProxyUtils.ultimateTargetClass(consumerBean); // unwraps any proxy
    CodelabJobSubscription subscription =
        AnnotatedElementUtils.findMergedAnnotation(targetClass, CodelabJobSubscription.class);
    Assert.state(
        subscription != null,
        () ->
            "Bean '%s' implements CodelabJobConsumer but has no @CodelabJobSubscription"
                .formatted(beanName));

    ResolvedConsumer<V> resolved = resolve(targetClass, beanName, CodelabJobConsumer.class);

    DeadLetterPolicy deadLetterPolicy =
        resolveDeadLetterPolicy(subscription.deadLetterPolicyRef(), targetClass);

    CodelabJobMessageDispatcher<V> dispatcher =
        new CodelabJobMessageDispatcher<>(
            (CodelabJobConsumer<V>) consumerBean, resolved.payloadType);

    MethodPulsarListenerEndpoint<byte[]> endpoint =
        buildEndpoint(
            beanName + "-codelabJobListener",
            dispatcher,
            JOB_DISPATCH_METHOD,
            subscription.type(),
            subscription.concurrency(),
            subscription.initialPosition(),
            subscription.ackTimeoutSeconds(),
            deadLetterPolicy);
    endpoint.setTopics(resolved.topic);
    endpoint.setSubscriptionName(resolved.qualifiedSubscriptionName);

    log.debug(
        "Registered job listener {} for {} on {} (payload {}, DLQ policy {})",
        beanName,
        targetClass.getSimpleName(),
        resolved.topic,
        resolved.payloadType.getSimpleName(),
        deadLetterPolicy != null ? "native" : "none — failures redeliver indefinitely");

    return endpoint;
  }

  /**
   * Resolves the module-qualified topic, the qualified subscription name and the declared payload
   * type shared by both consumer paths.
   */
  private <V> ResolvedConsumer<V> resolve(
      Class<?> targetClass, String beanName, Class<?> contract) {
    String moduleName =
        (String)
            beanFactory
                .getBeanDefinition(beanName)
                .getAttribute(CodelabPulsarRegistryUtils.MODULE_ATTRIBUTE);
    Assert.state(
        StringUtils.hasText(moduleName),
        () ->
            "Bean '%s' has no module attribute — was it registered inside registerPulsarSubscriptions?"
                .formatted(beanName));

    String resolvedTopic;
    String subscriptionName;
    if (contract == CodelabEventConsumer.class) {
      CodelabSubscription subscription =
          AnnotatedElementUtils.findMergedAnnotation(targetClass, CodelabSubscription.class);
      resolvedTopic = CodelabTopicResolver.resolveTopicName(moduleName, subscription, environment);
      subscriptionName = subscription.subscriptionName();
    } else {
      CodelabJobSubscription subscription =
          AnnotatedElementUtils.findMergedAnnotation(targetClass, CodelabJobSubscription.class);
      resolvedTopic = CodelabTopicResolver.resolveTopicName(moduleName, subscription, environment);
      subscriptionName = subscription.subscriptionName();
    }

    Class<V> payloadType = payloadTypeOf(targetClass, contract);
    // keep validating the consume(T) shape even though the endpoint points at the dispatcher
    findConsumeMethod(targetClass);

    return new ResolvedConsumer<>(
        resolvedTopic,
        CodelabSubscriptionResolver.resolveSubscriptionName(moduleName, subscriptionName),
        payloadType);
  }

  private static <V> Class<V> payloadTypeOf(Class<?> targetClass, Class<?> contract) {
    Class<?> payloadType =
        ResolvableType.forClass(targetClass).as(contract).getGeneric(0).resolve();
    Assert.state(
        payloadType != null,
        () ->
            "Consumer '%s' must parameterize %s<T>"
                .formatted(targetClass.getName(), contract.getSimpleName()));
    return (Class<V>) payloadType;
  }

  /**
   * The endpoint skeleton shared by both paths: byte-array decoding (so dispatchers see raw message
   * properties/data) + record-level acking + optional native DeadLetterPolicy.
   */
  private MethodPulsarListenerEndpoint<byte[]> buildEndpoint(
      String id,
      Object dispatcherBean,
      Method dispatchMethod,
      SubscriptionType subscriptionType,
      int concurrency,
      SubscriptionInitialPosition initialPosition,
      int ackTimeoutSeconds,
      DeadLetterPolicy deadLetterPolicy) {
    MethodPulsarListenerEndpoint<byte[]> endpoint = new MethodPulsarListenerEndpoint<>();
    endpoint.setId(id);
    endpoint.setBean(dispatcherBean);
    endpoint.setMethod(dispatchMethod);
    endpoint.setMessageHandlerMethodFactory(messageHandlerMethodFactory);
    // SchemaType.NONE with the dispatcher's Message<byte[]> parameter resolves to Schema.BYTES
    // (DefaultSchemaResolver maps byte[] → BYTES), so both dispatchers work on raw message data.
    endpoint.setSchemaType(SchemaType.NONE);
    endpoint.setAckMode(AckMode.RECORD);
    endpoint.setSubscriptionType(subscriptionType);
    endpoint.setConcurrency(concurrency);

    // initialPosition and ackTimeout aren't first-class endpoint properties —
    // @PulsarListener applies them to the underlying ConsumerBuilder too.
    endpoint.setConsumerBuilderCustomizer(
        builder -> {
          builder.subscriptionInitialPosition(initialPosition);
          builder.ackTimeout(ackTimeoutSeconds, TimeUnit.SECONDS);
        });

    if (deadLetterPolicy != null) {
      endpoint.setDeadLetterPolicy(deadLetterPolicy);
    }

    return endpoint;
  }

  private DeadLetterPolicy resolveDeadLetterPolicy(
      String deadLetterPolicyRef, Class<?> targetClass) {
    if (!StringUtils.hasText(deadLetterPolicyRef)) {
      return null;
    }
    Object policyBean = beanFactory.getBean(deadLetterPolicyRef);
    Assert.state(
        policyBean instanceof DeadLetterPolicy,
        () ->
            "deadLetterPolicyRef '%s' on %s must be a DeadLetterPolicy bean"
                .formatted(deadLetterPolicyRef, targetClass.getName()));
    return (DeadLetterPolicy) policyBean;
  }

  private Method findConsumeMethod(Class<?> targetClass) {
    // CodelabEventConsumer<T>.consume(T) / CodelabJobConsumer<T>.consume(T) produce a synthetic
    // bridge method (consume(Object)) alongside the real one — filter that out.
    return Arrays.stream(targetClass.getMethods())
        .filter(
            m ->
                m.getName().equals("consume")
                    && m.getParameterCount() == 1
                    && !m.isBridge()
                    && !m.isSynthetic())
        .findFirst()
        .orElseThrow(
            () ->
                new IllegalStateException(
                    "No concrete consume(T) method found on " + targetClass.getName()));
  }

  /** Topic + qualified subscription + payload type resolved for a consumer bean. */
  private record ResolvedConsumer<V>(
      String topic, String qualifiedSubscriptionName, Class<V> payloadType) {}
}
