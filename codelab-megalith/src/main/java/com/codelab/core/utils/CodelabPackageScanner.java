package com.codelab.core.utils;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.filter.AssignableTypeFilter;

import java.lang.annotation.Annotation;
import java.util.Set;
import java.util.stream.Collectors;

public class CodelabPackageScanner {

  /** List interfaces within basePackage that extend superClass */
  public static <T> Set<Class<?>> scan(String basePackage, Class<T> superClass, boolean includeInterfaces, boolean includeConcreteClasses) {
    ClassPathScanningCandidateComponentProvider scanner =
        new ClassPathScanningCandidateComponentProvider(false) {
          @Override
          protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
            // default impl excludes non-concrete classes; override to allow interfaces
              AnnotationMetadata metadata = beanDefinition.getMetadata();
            return metadata.isIndependent() && (
                    (includeInterfaces && metadata.isInterface())
                    || (includeConcreteClasses && metadata.isConcrete())
            );
          }
        };

    // Scan for anything assignable to CodelabEventConsumer (interfaces included)
    scanner.addIncludeFilter(new AssignableTypeFilter(superClass));

    return scanner.findCandidateComponents(basePackage).stream()
        .map(
            beanDef -> {
              try {
                return Class.forName(beanDef.getBeanClassName());
              } catch (ClassNotFoundException e) {
                throw new BeanDefinitionStoreException(
                    "Failed to load class: " + beanDef.getBeanClassName(), e);
              }
            })
        .filter(
            clazz -> {
              // Only interested in interfaces that actually extend superClass
              // (AssignableTypeFilter will also match superClass itself, and any
              // concrete implementations if present on the classpath — filter those out)
              return (!includeInterfaces || clazz.isInterface()) && !clazz.equals(superClass);
            })
        .collect(Collectors.toSet());
  }

  public static <T, A extends Annotation> A validateAndGetAnnotation(
      Class<T> clazz, Class<A> requiredAnnotation) {

    // CodelabSubscription ann = clazz.getAnnotation(CodelabSubscription.class);
    A ann = AnnotatedElementUtils.findMergedAnnotation(clazz, requiredAnnotation);
    if (ann == null) {
      throw new IllegalStateException(
          "Interface "
              + clazz.getName()
              + " extends CodelabEventConsumer but is "
              + "missing the required @CodelabSubscription annotation.");
    }
    return ann;
  }

  // resolve T from CodelabEventPublisher<T> up the interface hierarchy
  public static ResolvableType validateAndResolvePayloadType(Class<?> clazz, Class<?> annotation) {
    ResolvableType type = ResolvableType.forClass(clazz).as(annotation).getGeneric(0);
    if (type.resolve() == null) {
      throw new IllegalStateException(
          clazz.getName() + " must parameterize " + annotation.getSimpleName());
    }
    return type;
  }
}
