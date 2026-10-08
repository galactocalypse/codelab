package com.codelab.core.eventbus;

import com.codelab.common.spring.eventbus.BusinessVersion;
import com.codelab.common.spring.eventbus.CodelabEventConsumer;
import com.codelab.common.spring.eventbus.CodelabSubscription;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.pulsar.client.api.DeadLetterPolicy;
import org.apache.pulsar.client.api.Message;
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

@Slf4j
@RequiredArgsConstructor
public class CodelabPulsarListenerConfigurer
    implements PulsarListenerConfigurer, ResourceLoaderAware, EnvironmentAware {

  /** The framework listener method: version gate + payload decode + DLQ routing. */
  private static final Method DISPATCH_METHOD =
      ReflectionUtils.findMethod(
          CodelabVersionAwareMessageDispatcher.class, "dispatch", Message.class);

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
                registrar.registerEndpoint(buildEndpoint(beanName, consumerBean), null));
    // passing null for the factory means "use the default pulsarListenerContainerFactory",
    // same as an @PulsarListener with no containerFactory() set.
  }

  @SuppressWarnings("unchecked")
  private <V> PulsarListenerEndpoint buildEndpoint(
      String beanName, CodelabEventConsumer<?> consumerBean) {
    Class<?> targetClass = AopProxyUtils.ultimateTargetClass(consumerBean); // unwraps any proxy
    CodelabSubscription subscription =
        AnnotatedElementUtils.findMergedAnnotation(targetClass, CodelabSubscription.class);
    Assert.state(
        subscription != null,
        () ->
            "Bean '%s' implements CodelabEventConsumer but has no @CodelabSubscription"
                .formatted(beanName));

    String moduleName =
        (String)
            beanFactory
                .getBeanDefinition(beanName)
                .getAttribute(CodelabPulsarRegistryUtils.MODULE_ATTRIBUTE);
    Assert.state(
        StringUtils.hasText(moduleName),
        () ->
            "Bean '%s' has no module attribute — was it registered outside registerPulsarSubscriptions?"
                .formatted(beanName));
    String resolvedTopic =
        CodelabTopicResolver.resolveTopicName(moduleName, subscription, environment);
    String qualifiedSubscriptionName =
        CodelabSubscriptionResolver.resolveSubscriptionName(
            moduleName, subscription.subscriptionName());

    // The version contract is enforced at startup: a consumer that can't handle *any* version —
    // or can't state its payload type — is a wiring bug, not a runtime surprise.
    Set<BusinessVersion> supportedVersions = consumerBean.getSupportedBusinessVersions();
    Assert.notEmpty(
        supportedVersions,
        () ->
            "Consumer '%s' declares no supported business versions"
                .formatted(targetClass.getSimpleName()));
    Class<?> payloadType =
        ResolvableType.forClass(targetClass).as(CodelabEventConsumer.class).getGeneric(0).resolve();
    Assert.state(
        payloadType != null,
        () ->
            "Consumer '%s' must parameterize CodelabEventConsumer<T>"
                .formatted(targetClass.getName()));
    // keep validating the consume(T) shape even though the endpoint now points at the dispatcher
    findConsumeMethod(targetClass);

    DeadLetterPolicy deadLetterPolicy =
        resolveDeadLetterPolicy(subscription, beanName, targetClass);
    String dlqTopic =
        deadLetterPolicy != null && StringUtils.hasText(deadLetterPolicy.getDeadLetterTopic())
            ? deadLetterPolicy.getDeadLetterTopic()
            // mirrors Pulsar's own convention (RetryMessageUtil.getDLQTopic): topic + "-" + sub +
            // "-DLQ"
            : resolvedTopic + "-" + qualifiedSubscriptionName + "-DLQ";

    CodelabVersionAwareMessageDispatcher<V> dispatcher =
        new CodelabVersionAwareMessageDispatcher<>(
            (CodelabEventConsumer<V>) consumerBean,
            (Class<V>) payloadType,
            supportedVersions,
            dlqTopic,
            qualifiedSubscriptionName,
            codelabDlqRouter);

    MethodPulsarListenerEndpoint<byte[]> endpoint = new MethodPulsarListenerEndpoint<>();
    endpoint.setId(beanName + "-codelabListener");
    endpoint.setBean(dispatcher);
    endpoint.setMethod(DISPATCH_METHOD);
    endpoint.setMessageHandlerMethodFactory(messageHandlerMethodFactory);
    // SchemaType.NONE with the dispatcher's Message<byte[]> parameter resolves to Schema.BYTES
    // (DefaultSchemaResolver maps byte[] → BYTES), so the version gate runs on message
    // properties before any deserialization; the dispatcher decodes with Schema.JSON(payloadType).
    endpoint.setSchemaType(SchemaType.NONE);
    endpoint.setAckMode(AckMode.RECORD);

    endpoint.setTopics(resolvedTopic);
    endpoint.setSubscriptionName(qualifiedSubscriptionName);
    endpoint.setSubscriptionType(subscription.type());
    endpoint.setConcurrency(subscription.concurrency());

    // initialPosition and ackTimeout aren't first-class endpoint properties —
    // @PulsarListener applies them to the underlying ConsumerBuilder too.
    endpoint.setConsumerBuilderCustomizer(
        builder -> {
          builder.subscriptionInitialPosition(subscription.initialPosition());
          builder.ackTimeout(subscription.ackTimeoutSeconds(), TimeUnit.SECONDS);
        });

    if (deadLetterPolicy != null) {
      endpoint.setDeadLetterPolicy(deadLetterPolicy);
    }

    log.debug(
        "Registered listener {} for {} on {} (payload {}, versions {}, DLQ {})",
        beanName,
        targetClass.getSimpleName(),
        resolvedTopic,
        payloadType.getSimpleName(),
        supportedVersions,
        dlqTopic);

    return endpoint;
  }

  private DeadLetterPolicy resolveDeadLetterPolicy(
      CodelabSubscription subscription, String beanName, Class<?> targetClass) {
    if (!StringUtils.hasText(subscription.deadLetterPolicyRef())) {
      return null;
    }
    Object policyBean = beanFactory.getBean(subscription.deadLetterPolicyRef());
    Assert.state(
        policyBean instanceof DeadLetterPolicy,
        () ->
            "deadLetterPolicyRef '%s' on %s must be a DeadLetterPolicy bean"
                .formatted(subscription.deadLetterPolicyRef(), targetClass.getName()));
    return (DeadLetterPolicy) policyBean;
  }

  private Method findConsumeMethod(Class<?> targetClass) {
    // CodelabEventConsumer<T>.consume(T) produces a synthetic bridge method
    // (consume(Object)) alongside the real one — filter that out.
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
}
