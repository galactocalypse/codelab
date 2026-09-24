package com.codelab.core.eventbus;

import com.codelab.common.spring.eventbus.CodelabEventConsumer;
import com.codelab.common.spring.eventbus.CodelabEventPublisher;
import com.codelab.common.spring.eventbus.CodelabSubscription;
import com.codelab.common.spring.eventbus.CodelabTopic;
import com.codelab.common.spring.persistence.CodelabModule;
import com.codelab.core.CodelabBeanNaming;
import com.codelab.core.CodelabModuleRegistrationContext;
import com.codelab.core.utils.CodelabPackageScanner;
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
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.util.ClassUtils;

@Slf4j
public class CodelabPulsarRegistryUtils {

  public static final String MODULE_ATTRIBUTE = "codelab.pulsar.module";

  public static void registerPulsarPublishers(
      CodelabModule module, CodelabModuleRegistrationContext context) {

    ResourceLoader resourceLoader = context.getResourceLoader();
    ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
    Environment environment = context.getEnvironment();
    BeanDefinitionRegistry registry = context.getRegistry();

    ClassPathScanningCandidateComponentProvider scanner =
        new ClassPathScanningCandidateComponentProvider(false);
    scanner.setResourceLoader(resourceLoader);
    scanner.addIncludeFilter(new AssignableTypeFilter(CodelabEventPublisher.class));
    // interfaces don't match AssignableTypeFilter's default component check,
    // so override isCandidateComponent to accept interfaces too
    scanner =
        new ClassPathScanningCandidateComponentProvider(false) {
          @Override
          protected boolean isCandidateComponent(AnnotatedBeanDefinition bd) {
            return bd.getMetadata().isInterface() && bd.getMetadata().isIndependent();
          }
        };
    scanner.addIncludeFilter(new AssignableTypeFilter(CodelabEventPublisher.class));
    CodelabTopicRegistry topicRegistry = new CodelabTopicRegistry();
    Set<BeanDefinition> candidateComponents =
        module.basePackage() == null
            ? Set.of()
            : scanner.findCandidateComponents(module.basePackage());
    for (BeanDefinition candidate : candidateComponents) {
      AnnotatedBeanDefinition abd = (AnnotatedBeanDefinition) candidate;
      AnnotationMetadata metadata = abd.getMetadata();

      if (!metadata.isInterface()) continue;

      if (metadata.getAnnotationAttributes(CodelabTopic.class.getName()) == null) {
        throw new IllegalStateException(
            metadata.getClassName()
                + " extends CodelabEventPublisher but is missing @CodelabTopic");
      }

      // now that you KNOW this is a real candidate you're registering,
      // resolve the class — this is the one place you actually need it
      Class<?> publisherIface =
          ClassUtils.resolveClassName(metadata.getClassName(), beanFactory.getBeanClassLoader());

      CodelabTopic topicAnno =
          AnnotatedElementUtils.findMergedAnnotation(publisherIface, CodelabTopic.class);
      if (topicAnno == null) {
        throw new IllegalStateException(
            publisherIface.getName()
                + " extends CodelabEventPublisher but is missing @CodelabTopic");
      }

      // resolve T from CodelabEventPublisher<T> up the interface hierarchy
      ResolvableType payloadType =
          ResolvableType.forClass(publisherIface).as(CodelabEventPublisher.class).getGeneric(0);
      if (payloadType.resolve() == null) {
        throw new IllegalStateException(
            publisherIface.getName() + " must parameterize CodelabEventPublisher<T>");
      }

      String resolvedTopic =
          CodelabTopicResolver.resolveTopicName(
              module, topicAnno, environment); // tenant + namespace + topic
      topicRegistry.validateNoDuplicateTopic(
          resolvedTopic, publisherIface); // fail fast, per earlier point

      BeanDefinitionBuilder bdBuilder =
          BeanDefinitionBuilder.genericBeanDefinition(CodelabPublisherFactoryBean.class)
              .addConstructorArgValue(publisherIface)
              .addConstructorArgValue(resolvedTopic)
              .addConstructorArgValue(payloadType.resolve())
              .setLazyInit(false); // producers should connect at startup, not first publish

      String beanName = CodelabBeanNaming.deriveBeanName(module, publisherIface);
      registry.registerBeanDefinition(beanName, bdBuilder.getBeanDefinition());
    }
  }

  public static void registerPulsarSubscriptions(
      CodelabModule module, CodelabModuleRegistrationContext context) {
    if (StringUtils.isBlank(module.basePackage())) {
      return;
    }
    Set<Class<?>> candidates =
        CodelabPackageScanner.scan(module.basePackage(), CodelabEventConsumer.class, false, true);
    for (Class<?> candidate : candidates) {
      CodelabPackageScanner.validateAndGetAnnotation(candidate, CodelabSubscription.class);
      BeanDefinition definition =
          BeanDefinitionBuilder.genericBeanDefinition(candidate)
              .setScope(BeanDefinition.SCOPE_SINGLETON)
              .getBeanDefinition();
      definition.setAttribute(MODULE_ATTRIBUTE, module.name());
      String beanName = CodelabBeanNaming.deriveBeanName(module, candidate.getSimpleName());
      context.getRegistry().registerBeanDefinition(beanName, definition);
    }
  }
}
