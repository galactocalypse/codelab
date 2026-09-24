package com.codelab.core;

import com.codelab.common.spring.persistence.CodelabModule;
import com.codelab.core.eventbus.CodelabPulsarRegistryUtils;
import com.codelab.core.jpa.CodelabJpaRegistryUtils;
import java.util.List;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;

@Slf4j
public class CodelabModuleRegistrar
    implements BeanDefinitionRegistryPostProcessor,
        ResourceLoaderAware,
        EnvironmentAware,
        BeanFactoryAware {

  private ConfigurableListableBeanFactory beanFactory;
  @Setter private ResourceLoader resourceLoader;
  @Setter private Environment environment;

  @Override
  public void setBeanFactory(@NonNull BeanFactory beanFactory) {
    this.beanFactory = (ConfigurableListableBeanFactory) beanFactory;
  }

  @Override
  public void postProcessBeanDefinitionRegistry(@NonNull BeanDefinitionRegistry registry) {
    CodelabModuleRegistrationContext context =
        CodelabModuleRegistrationContext.builder()
            .beanFactory(beanFactory)
            .resourceLoader(resourceLoader)
            .environment(environment)
            .registry(registry)
            .build();
    List<CodelabModule> modules = ModuleUtils.loadModules(resourceLoader);

    for (CodelabModule module : modules) {
      CodelabJpaRegistryUtils.registerJpaBeans(module, context);
      CodelabPulsarRegistryUtils.registerPulsarPublishers(module, context);
      CodelabPulsarRegistryUtils.registerPulsarSubscriptions(module, context);
    }
  }

  @Override
  public void postProcessBeanFactory(@NonNull ConfigurableListableBeanFactory beanFactory) {
    /* no-op */
  }
}
