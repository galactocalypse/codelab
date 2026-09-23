package com.codelab.core.jpa;

import com.codelab.common.spring.persistence.CodelabModule;
import com.codelab.core.CodelabModuleRegistrationContext;
import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.AnnotationBeanNameGenerator;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.jpa.repository.config.JpaRepositoryConfigExtension;
import org.springframework.data.repository.config.RepositoryConfigurationDelegate;
import org.springframework.data.repository.config.RepositoryConfigurationExtension;
import org.springframework.data.repository.config.RepositoryConfigurationSource;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;

@Slf4j
public class CodelabJpaRegistryUtils {

  public static void registerJpaBeans(
      CodelabModule module, CodelabModuleRegistrationContext context) {
    registerDataSource(module, context);
    registerEntityManagerFactory(module, context);
    registerTransactionManager(module, context);
    registerRepositories(module, context);
  }

  private static void registerDataSource(
      CodelabModule module, CodelabModuleRegistrationContext context) {
    BeanDefinitionRegistry registry = context.getRegistry();
    Environment environment = context.getEnvironment();
    BeanDefinition def =
        BeanDefinitionBuilder.genericBeanDefinition(
                DataSource.class, () -> CodelabDataSourceFactory.create(environment, module))
            .setDestroyMethodName("close")
            .getBeanDefinition();

    registry.registerBeanDefinition(JpaBeanNaming.dataSourceBeanName(module), def);
  }

  private static void registerEntityManagerFactory(
      CodelabModule module, CodelabModuleRegistrationContext context) {
    BeanDefinitionRegistry registry = context.getRegistry();
    ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
    BeanDefinition def =
        BeanDefinitionBuilder.genericBeanDefinition(
                LocalContainerEntityManagerFactoryBean.class,
                () -> {
                  EntityManagerFactoryBuilder builder =
                      beanFactory.getBean(EntityManagerFactoryBuilder.class);

                  DataSource dataSource =
                      beanFactory.getBean(
                          JpaBeanNaming.dataSourceBeanName(module), DataSource.class);

                  return builder
                      .dataSource(dataSource)
                      .packages(module.basePackage())
                      .persistenceUnit(module.persistenceUnit())
                      .build();
                })
            .getBeanDefinition();

    registry.registerBeanDefinition(JpaBeanNaming.emfBeanName(module), def);
  }

  private static void registerTransactionManager(
      CodelabModule module, CodelabModuleRegistrationContext context) {
    BeanDefinitionRegistry registry = context.getRegistry();
    BeanDefinition def =
        BeanDefinitionBuilder.rootBeanDefinition(JpaTransactionManager.class)
            .addPropertyReference("entityManagerFactory", JpaBeanNaming.emfBeanName(module))
            .getBeanDefinition();

    registry.registerBeanDefinition(JpaBeanNaming.txBeanName(module), def);
  }

  private static void registerRepositories(
      CodelabModule module, CodelabModuleRegistrationContext context) {
    BeanDefinitionRegistry registry = context.getRegistry();
    Environment environment = context.getEnvironment();
    ResourceLoader resourceLoader = context.getResourceLoader();
    RepositoryConfigurationSource source =
        new CodelabJpaRepositoryConfigurationSource(
            module, environment, resourceLoader, registry, AnnotationBeanNameGenerator.INSTANCE);
    RepositoryConfigurationExtension extension = new JpaRepositoryConfigExtension();
    RepositoryConfigurationDelegate delegate =
        new RepositoryConfigurationDelegate(source, resourceLoader, environment);
    delegate.registerRepositoriesIn(registry, extension);
  }
}
