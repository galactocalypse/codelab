package com.codelab.megalith;

import com.codelab.core.CodelabModuleRegistrar;
import com.codelab.core.eventbus.CodelabPulsarListenerConfigurer;
import com.codelab.core.eventbus.CodelabPulsarRegistryUtils;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.handler.annotation.support.DefaultMessageHandlerMethodFactory;
import org.springframework.messaging.handler.annotation.support.MessageHandlerMethodFactory;

@Configuration
public class MegalithConfiguration {

  @Bean
  public static CodelabModuleRegistrar codelabModuleRegistrar() {
    return new CodelabModuleRegistrar();
  }

  @Bean
  public CodelabPulsarListenerConfigurer pulsarListenerConfigurer(
      ConfigurableListableBeanFactory beanFactory,
      MessageHandlerMethodFactory codelabPulsarHandlerMethodFactory) {
    return new CodelabPulsarListenerConfigurer(beanFactory, codelabPulsarHandlerMethodFactory);
  }

  @Bean
  public MessageHandlerMethodFactory codelabPulsarHandlerMethodFactory() {
    return new DefaultMessageHandlerMethodFactory();
  }
}
