package com.codelab.core.eventbus;

import com.codelab.common.spring.eventbus.CodelabEventConsumer;
import com.codelab.common.spring.eventbus.CodelabSubscription;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import com.codelab.common.spring.persistence.CodelabModule;
import com.codelab.core.ModuleUtils;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.pulsar.client.api.DeadLetterPolicy;
import org.apache.pulsar.common.schema.SchemaType;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
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
import org.springframework.util.StringUtils;


@Slf4j
@RequiredArgsConstructor
public class CodelabPulsarListenerConfigurer implements PulsarListenerConfigurer, ResourceLoaderAware, EnvironmentAware {

  private final ConfigurableListableBeanFactory beanFactory;
  private final MessageHandlerMethodFactory messageHandlerMethodFactory;

  @Setter
  private Environment environment;
  @Setter
  private ResourceLoader resourceLoader;

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

    String moduleName = (String) beanFactory.getBeanDefinition(beanName)
            .getAttribute(CodelabPulsarRegistryUtils.MODULE_ATTRIBUTE);
    Assert.state(StringUtils.hasText(moduleName),
            () -> "Bean '%s' has no module attribute — was it registered outside registerPulsarSubscriptions?"
                    .formatted(beanName));
    String resolvedTopic = CodelabTopicResolver.resolveTopicName(
            moduleName, subscription, environment);

    MethodPulsarListenerEndpoint<V> endpoint = new MethodPulsarListenerEndpoint<>();
    endpoint.setId(beanName + "-codelabListener");
    endpoint.setBean(consumerBean);
    endpoint.setMethod(findConsumeMethod(targetClass));
    endpoint.setMessageHandlerMethodFactory(messageHandlerMethodFactory);
    endpoint.setSchemaType(SchemaType.NONE);
    endpoint.setAckMode(AckMode.RECORD);

    endpoint.setTopics(resolvedTopic);
    endpoint.setSubscriptionName(CodelabSubscriptionResolver.resolveSubscriptionName(moduleName, subscription.subscriptionName()));
    endpoint.setSubscriptionType(subscription.type());
    endpoint.setConcurrency(subscription.concurrency());

    // initialPosition and ackTimeout aren't first-class endpoint properties —
    // @PulsarListener applies them to the underlying ConsumerBuilder too.
    endpoint.setConsumerBuilderCustomizer(
        builder -> {
          builder.subscriptionInitialPosition(subscription.initialPosition());
          builder.ackTimeout(subscription.ackTimeoutSeconds(), TimeUnit.SECONDS);
        });

    if (StringUtils.hasText(subscription.deadLetterPolicyRef())) {
      Object policyBean = beanFactory.getBean(subscription.deadLetterPolicyRef());
      Assert.state(
          policyBean instanceof DeadLetterPolicy,
          () ->
              "deadLetterPolicyRef '%s' on %s must be a DeadLetterPolicy bean"
                  .formatted(subscription.deadLetterPolicyRef(), targetClass.getName()));
      endpoint.setDeadLetterPolicy((DeadLetterPolicy) policyBean);
    }

    return endpoint;
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
