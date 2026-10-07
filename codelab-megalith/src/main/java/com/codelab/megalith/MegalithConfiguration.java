package com.codelab.megalith;

import com.codelab.core.CodelabModuleRegistrar;
import com.codelab.core.eventbus.CodelabPulsarListenerConfigurer;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.handler.annotation.support.DefaultMessageHandlerMethodFactory;
import org.springframework.messaging.handler.annotation.support.MessageHandlerMethodFactory;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
// DataSourceAutoConfiguration is excluded, so Boot's TransactionAutoConfiguration backs off and
// annotation-driven transactions would otherwise be silently absent: @Transactional methods (e.g.
// TmdbMovieImportService.persist) would run untransacted, each repository call in its own flushed
// session — findById then returns a detached entity and re-imports blow up on lazy collections.
// Enabling it here makes the qualifier'd per-module TransactionManagers take effect.
@EnableTransactionManagement
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
