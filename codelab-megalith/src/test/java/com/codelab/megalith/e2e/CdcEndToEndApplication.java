package com.codelab.megalith.e2e;

import com.codelab.common.spring.persistence.CodelabModule;
import com.codelab.core.CodelabModuleRegistrationContext;
import com.codelab.core.eventbus.CodelabDlqRouter;
import com.codelab.core.eventbus.CodelabPulsarListenerConfigurer;
import com.codelab.core.eventbus.CodelabPulsarRegistryUtils;
import com.codelab.movies.autoconfigure.MoviesAutoConfiguration;
import com.codelab.orders.autoconfigure.OrdersAutoConfiguration;
import java.util.Map;
import org.apache.pulsar.client.api.DeadLetterPolicy;
import org.apache.pulsar.client.api.PulsarClient;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.messaging.handler.annotation.support.DefaultMessageHandlerMethodFactory;
import org.springframework.messaging.handler.annotation.support.MessageHandlerMethodFactory;

/**
 * Minimal Spring Boot application for the CDC end-to-end test.
 *
 * <p>It deliberately does <em>not</em> boot the full megalith: JPA/DataSource autoconfiguration and
 * the module service autoconfigurations are excluded, because the CDC→job→event loop touches no
 * database. What it does keep is the real messaging wiring — the strong parts of the framework: the
 * {@link CodelabPulsarRegistryUtils registration scanner}, the {@link
 * CodelabPulsarListenerConfigurer listener configurer}, both dispatchers, the DLQ router and the
 * kind-prefixed {@code CodelabTopicResolver} — so the test exercises the production publish/consume
 * paths against a real broker.
 *
 * <p>{@link MessagingOnlyRegistrar} registers the real {@code orders} module (its event publishers
 * and the {@code OrderCdcNormalizer} job consumer) the same way {@code CodelabModuleRegistrar}
 * does, minus the JPA half, and additionally registers the test's recording event consumer.
 */
@SpringBootConfiguration
@EnableAutoConfiguration(
    exclude = {
      DataSourceAutoConfiguration.class,
      HibernateJpaAutoConfiguration.class,
      DataJpaRepositoriesAutoConfiguration.class,
      OrdersAutoConfiguration.class,
      MoviesAutoConfiguration.class
    })
public class CdcEndToEndApplication {

  @Bean
  public static MessagingOnlyRegistrar codelabMessagingOnlyRegistrar() {
    return new MessagingOnlyRegistrar();
  }

  @Bean
  public CodelabPulsarListenerConfigurer pulsarListenerConfigurer(
      ConfigurableListableBeanFactory beanFactory,
      MessageHandlerMethodFactory codelabPulsarHandlerMethodFactory,
      CodelabDlqRouter codelabDlqRouter) {
    return new CodelabPulsarListenerConfigurer(
        beanFactory, codelabPulsarHandlerMethodFactory, codelabDlqRouter);
  }

  @Bean
  public CodelabDlqRouter codelabDlqRouter(PulsarClient pulsarClient) {
    return new CodelabDlqRouter(pulsarClient);
  }

  @Bean
  public MessageHandlerMethodFactory codelabPulsarHandlerMethodFactory() {
    return new DefaultMessageHandlerMethodFactory();
  }

  /**
   * Referenced by {@code OrderCdcNormalizer}'s {@code deadLetterPolicyRef}; mirrors the real bean.
   */
  @Bean
  public DeadLetterPolicy orderCdcDeadLetterPolicy() {
    return DeadLetterPolicy.builder().maxRedeliverCount(3).build();
  }

  static class MessagingOnlyRegistrar
      implements BeanDefinitionRegistryPostProcessor,
          ResourceLoaderAware,
          EnvironmentAware,
          BeanFactoryAware {

    private ConfigurableListableBeanFactory beanFactory;
    private ResourceLoader resourceLoader;
    private Environment environment;

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
      this.beanFactory = (ConfigurableListableBeanFactory) beanFactory;
    }

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
      this.resourceLoader = resourceLoader;
    }

    @Override
    public void setEnvironment(Environment environment) {
      this.environment = environment;
    }

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) {
      CodelabModuleRegistrationContext context =
          CodelabModuleRegistrationContext.builder()
              .beanFactory(beanFactory)
              .resourceLoader(resourceLoader)
              .environment(environment)
              .registry(registry)
              .build();
      // Same coordinates OrdersModuleProvider declares, so the scanner finds the real publishers
      // and consumers under com.codelab.orders and the topics resolve the production way.
      CodelabModule orders =
          new CodelabModule("orders", "com.codelab.orders", "ordersPU", "orders", Map.of());

      CodelabPulsarRegistryUtils.registerPulsarPublishers(orders, context);
      CodelabPulsarRegistryUtils.registerPulsarSubscriptions(orders, context);

      // The job-bus E2E fixtures live in a subpackage of the test tree. Registering them as another
      // module on the same "orders" tenant (the one this container provisions) runs them through
      // the real job publisher/subscription scanners without pulling in the JPA-backed module
      // services.
      CodelabModule jobFixtures =
          new CodelabModule("orders", "com.codelab.megalith.e2e.jobs", "n/a", "n/a", Map.of());
      CodelabPulsarRegistryUtils.registerPulsarPublishers(jobFixtures, context);
      CodelabPulsarRegistryUtils.registerPulsarSubscriptions(jobFixtures, context);

      // The observer is test scaffolding, so it's registered directly (with the module attribute
      // the configurer requires) rather than being discovered by the package scan.
      BeanDefinition observer =
          BeanDefinitionBuilder.genericBeanDefinition(RecordingOrderCreatedEventConsumer.class)
              .setScope(BeanDefinition.SCOPE_SINGLETON)
              .getBeanDefinition();
      observer.setAttribute(CodelabPulsarRegistryUtils.MODULE_ATTRIBUTE, orders.name());
      registry.registerBeanDefinition("orders.recordingOrderCreatedEventObserver", observer);
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
      /* no-op */
    }
  }
}
