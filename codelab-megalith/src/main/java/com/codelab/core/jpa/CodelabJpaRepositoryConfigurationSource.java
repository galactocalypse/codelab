package com.codelab.core.jpa;

import com.codelab.common.spring.persistence.CodelabModule;
import java.util.Optional;
import org.jspecify.annotations.NullMarked;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.repository.config.AnnotationRepositoryConfigurationSource;
import org.springframework.data.util.Streamable;

@NullMarked
final class CodelabJpaRepositoryConfigurationSource
    extends AnnotationRepositoryConfigurationSource {

  private final CodelabModule module;

  CodelabJpaRepositoryConfigurationSource(
      CodelabModule module,
      Environment environment,
      ResourceLoader resourceLoader,
      BeanDefinitionRegistry registry,
      BeanNameGenerator beanNameGenerator) {

    super(
        AnnotationMetadata.introspect(CodelabJpaRepositoriesConfiguration.class),
        EnableJpaRepositories.class,
        resourceLoader,
        environment,
        registry,
        beanNameGenerator);

    this.module = module;
  }

  @Override
  public Streamable<String> getBasePackages() {
    return Streamable.of(module.basePackage());
  }

  @Override
  public Object getSource() {
    return module;
  }

  @Override
  public <T> Optional<T> getAttribute(String name, Class<T> type) {

    return switch (name) {
      case "entityManagerFactoryRef" -> Optional.of(type.cast(JpaBeanNaming.emfBeanName(module)));

      case "transactionManagerRef" -> Optional.of(type.cast(JpaBeanNaming.txBeanName(module)));

      default -> super.getAttribute(name, type);
    };
  }

  @Override
  public Optional<String> getAttribute(String name) {
    return getAttribute(name, String.class);
  }

  @Override
  public String getResourceDescription() {
    return "CodelabModule[" + module.name() + "]";
  }
}
