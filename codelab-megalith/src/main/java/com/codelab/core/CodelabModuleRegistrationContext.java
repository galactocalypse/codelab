package com.codelab.core;

import lombok.Builder;
import lombok.Data;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;

@Data
@Builder
public class CodelabModuleRegistrationContext {

  private BeanDefinitionRegistry registry;
  private ConfigurableListableBeanFactory beanFactory;
  private ResourceLoader resourceLoader;
  private Environment environment;
}
