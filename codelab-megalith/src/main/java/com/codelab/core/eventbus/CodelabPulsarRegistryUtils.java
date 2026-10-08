package com.codelab.core.eventbus;

import com.codelab.common.spring.eventbus.CodelabEventConsumer;
import com.codelab.common.spring.eventbus.CodelabEventPublisher;
import com.codelab.common.spring.eventbus.CodelabSubscription;
import com.codelab.common.spring.eventbus.CodelabTopic;
import com.codelab.common.spring.jobbus.CodelabJobConsumer;
import com.codelab.common.spring.jobbus.CodelabJobPublisher;
import com.codelab.common.spring.jobbus.CodelabJobSubscription;
import com.codelab.common.spring.jobbus.CodelabJobTopic;
import com.codelab.common.spring.persistence.CodelabModule;
import com.codelab.core.CodelabBeanNaming;
import com.codelab.core.CodelabModuleRegistrationContext;
import com.codelab.core.utils.CodelabPackageScanner;
import java.lang.annotation.Annotation;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.util.ClassUtils;
import org.springframework.util.unit.DataSize;

@Slf4j
public class CodelabPulsarRegistryUtils {

  public static final String MODULE_ATTRIBUTE = "codelab.pulsar.module";

  /** App-level publish size cap for the event workflow (business events). */
  public static final String EVENTS_MAX_MESSAGE_SIZE_PROPERTY =
      "app.pulsar.events.max-message-size";

  /** App-level publish size cap for the job workflow (heavy async work). */
  public static final String JOBS_MAX_MESSAGE_SIZE_PROPERTY = "app.pulsar.jobs.max-message-size";

  private static final String DEFAULT_EVENTS_MAX_MESSAGE_SIZE = "1KB";
  private static final String DEFAULT_JOBS_MAX_MESSAGE_SIZE = "100KB";

  public static void registerPulsarPublishers(
      CodelabModule module, CodelabModuleRegistrationContext context) {
    Environment environment = context.getEnvironment();
    // one registry per module so a job topic and an event topic can't silently collide
    CodelabTopicRegistry topicRegistry = new CodelabTopicRegistry();

    registerPublishers(
        module,
        context,
        CodelabEventPublisher.class,
        CodelabTopic.class,
        CodelabPublisherFactoryBean.class,
        "event",
        maxMessageBytes(
            environment, EVENTS_MAX_MESSAGE_SIZE_PROPERTY, DEFAULT_EVENTS_MAX_MESSAGE_SIZE),
        topicRegistry);

    registerPublishers(
        module,
        context,
        CodelabJobPublisher.class,
        CodelabJobTopic.class,
        CodelabJobPublisherFactoryBean.class,
        "job",
        maxMessageBytes(environment, JOBS_MAX_MESSAGE_SIZE_PROPERTY, DEFAULT_JOBS_MAX_MESSAGE_SIZE),
        topicRegistry);
  }

  public static void registerPulsarSubscriptions(
      CodelabModule module, CodelabModuleRegistrationContext context) {
    if (StringUtils.isBlank(module.basePackage())) {
      return;
    }
    registerConsumers(module, context, CodelabEventConsumer.class, CodelabSubscription.class);
    registerConsumers(module, context, CodelabJobConsumer.class, CodelabJobSubscription.class);
  }

  private static long maxMessageBytes(
      Environment environment, String property, String defaultValue) {
    String raw = environment.getProperty(property, defaultValue);
    if (StringUtils.isBlank(raw)) {
      raw = defaultValue;
    }
    try {
      return DataSize.parse(raw.trim()).toBytes();
    } catch (IllegalArgumentException e) {
      throw new IllegalStateException(
          "Invalid %s value '%s' — expected a data size such as 1KB, 100KB or 1MB"
              .formatted(property, raw),
          e);
    }
  }

  private static <A extends Annotation> void registerPublishers(
      CodelabModule module,
      CodelabModuleRegistrationContext context,
      Class<?> contractType,
      Class<A> topicAnnotationType,
      Class<?> factoryBeanClass,
      String kind,
      long maxMessageBytes,
      CodelabTopicRegistry topicRegistry) {
    if (module.basePackage() == null) {
      return;
    }

    ResourceLoader resourceLoader = context.getResourceLoader();
    ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
    Environment environment = context.getEnvironment();
    BeanDefinitionRegistry registry = context.getRegistry();

    Set<BeanDefinition> candidateComponents =
        interfaceScanner(resourceLoader, contractType)
            .findCandidateComponents(module.basePackage());
    for (BeanDefinition candidate : candidateComponents) {
      AnnotatedBeanDefinition abd = (AnnotatedBeanDefinition) candidate;
      AnnotationMetadata metadata = abd.getMetadata();

      if (!metadata.isInterface()) continue;

      if (metadata.getAnnotationAttributes(topicAnnotationType.getName()) == null) {
        throw new IllegalStateException(
            metadata.getClassName()
                + " extends "
                + contractType.getSimpleName()
                + " but is missing @"
                + topicAnnotationType.getSimpleName());
      }

      // now that you KNOW this is a real candidate you're registering,
      // resolve the class — this is the one place you actually need it
      Class<?> publisherIface =
          ClassUtils.resolveClassName(metadata.getClassName(), beanFactory.getBeanClassLoader());

      A topicAnno = AnnotatedElementUtils.findMergedAnnotation(publisherIface, topicAnnotationType);
      if (topicAnno == null) {
        throw new IllegalStateException(
            publisherIface.getName()
                + " extends "
                + contractType.getSimpleName()
                + " but is missing @"
                + topicAnnotationType.getSimpleName());
      }

      // resolve T from the contract<T> up the interface hierarchy
      ResolvableType payloadType =
          ResolvableType.forClass(publisherIface).as(contractType).getGeneric(0);
      if (payloadType.resolve() == null) {
        throw new IllegalStateException(
            publisherIface.getName()
                + " must parameterize "
                + contractType.getSimpleName()
                + "<T>");
      }

      String resolvedTopic =
          CodelabTopicResolver.resolveTopicName(
              module.name(), logicalTopic(topicAnno), environment);
      topicRegistry.validateNoDuplicateTopic(resolvedTopic, publisherIface);

      BeanDefinitionBuilder bdBuilder =
          BeanDefinitionBuilder.genericBeanDefinition(factoryBeanClass)
              .addConstructorArgValue(publisherIface)
              .addConstructorArgValue(resolvedTopic)
              .addConstructorArgValue(payloadType.resolve())
              .addConstructorArgValue(maxMessageBytes)
              .setLazyInit(false); // producers should connect at startup, not first publish

      String beanName = CodelabBeanNaming.deriveBeanName(module, publisherIface);
      registry.registerBeanDefinition(beanName, bdBuilder.getBeanDefinition());
      log.debug(
          "Registered {} publisher {} on {} (payload {}, max {} bytes)",
          kind,
          beanName,
          resolvedTopic,
          payloadType.resolve().getSimpleName(),
          maxMessageBytes);
    }
  }

  private static <A extends Annotation> void registerConsumers(
      CodelabModule module,
      CodelabModuleRegistrationContext context,
      Class<?> contractType,
      Class<A> subscriptionAnnotationType) {
    Set<Class<?>> candidates =
        CodelabPackageScanner.scan(module.basePackage(), contractType, false, true);
    for (Class<?> candidate : candidates) {
      CodelabPackageScanner.validateAndGetAnnotation(candidate, subscriptionAnnotationType);
      BeanDefinition definition =
          BeanDefinitionBuilder.genericBeanDefinition(candidate)
              .setScope(BeanDefinition.SCOPE_SINGLETON)
              .getBeanDefinition();
      definition.setAttribute(MODULE_ATTRIBUTE, module.name());
      String beanName = CodelabBeanNaming.deriveBeanName(module, candidate.getSimpleName());
      context.getRegistry().registerBeanDefinition(beanName, definition);
    }
  }

  /** Scans for interfaces assignable to the given contract (concrete classes excluded). */
  private static ClassPathScanningCandidateComponentProvider interfaceScanner(
      ResourceLoader resourceLoader, Class<?> contractType) {
    ClassPathScanningCandidateComponentProvider scanner =
        new ClassPathScanningCandidateComponentProvider(false) {
          @Override
          protected boolean isCandidateComponent(AnnotatedBeanDefinition bd) {
            return bd.getMetadata().isInterface() && bd.getMetadata().isIndependent();
          }
        };
    scanner.setResourceLoader(resourceLoader);
    scanner.addIncludeFilter(new AssignableTypeFilter(contractType));
    return scanner;
  }

  /** Reads the effective topic name off a merged @CodelabTopic / @CodelabJobTopic annotation. */
  private static String logicalTopic(Annotation topicAnno) {
    Object value = AnnotationUtils.getValue(topicAnno, "value");
    if (value instanceof String text && StringUtils.isNotBlank(text)) {
      return text;
    }
    Object name = AnnotationUtils.getValue(topicAnno, "name");
    if (name instanceof String text && StringUtils.isNotBlank(text)) {
      return text;
    }
    throw new IllegalStateException(
        "@" + topicAnno.annotationType().getSimpleName() + " must specify a non-blank topic name");
  }
}
