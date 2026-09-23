package com.codelab.core;

import com.codelab.common.spring.persistence.CodelabModule;
import com.codelab.common.spring.persistence.CodelabModuleProvider;
import com.codelab.core.eventbus.CodelabPulsarRegistryUtils;
import com.codelab.core.jpa.CodelabJpaRegistryUtils;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.boot.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.AnnotationBeanNameGenerator;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.data.jpa.repository.config.JpaRepositoryConfigExtension;
import org.springframework.data.repository.config.RepositoryConfigurationDelegate;
import org.springframework.data.repository.config.RepositoryConfigurationExtension;
import org.springframework.data.repository.config.RepositoryConfigurationSource;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.List;

@Slf4j
public class CodelabModuleRegistrar
        implements  BeanDefinitionRegistryPostProcessor,
                    ResourceLoaderAware,
                    EnvironmentAware,
                    BeanFactoryAware {


    private ConfigurableListableBeanFactory beanFactory;
    @Setter
    private ResourceLoader resourceLoader;
    @Setter
    private Environment environment;


    @Override
    public void setBeanFactory(@NonNull BeanFactory beanFactory) {
        this.beanFactory = (ConfigurableListableBeanFactory) beanFactory;
    }

    @Override
    public void postProcessBeanDefinitionRegistry(@NonNull BeanDefinitionRegistry registry) {
        CodelabModuleRegistrationContext context = CodelabModuleRegistrationContext.builder()
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
    public void postProcessBeanFactory(@NonNull ConfigurableListableBeanFactory beanFactory) { /* no-op */ }




}
