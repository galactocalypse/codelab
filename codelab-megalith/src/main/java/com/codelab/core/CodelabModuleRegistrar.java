package com.codelab.core;

import com.codelab.common.spring.persistence.CodelabModule;
import com.codelab.common.spring.persistence.CodelabModuleProvider;
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

    private static final String VALIDATION_PROVIDER =
            "META-INF/services/jakarta.validation.spi.ValidationProvider";

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

        List<CodelabModule> modules = SpringFactoriesLoader
                .loadFactories(CodelabModuleProvider.class, resourceLoader.getClassLoader())
                .stream()
                .map(CodelabModuleProvider::provide)
                .toList();

        for (CodelabModule module : modules) {
            registerDataSource(registry, module);
            registerEntityManagerFactory(registry, module);
            registerTransactionManager(registry, module);
            registerRepositories(registry, module);
        }
    }

    private void registerDataSource(BeanDefinitionRegistry registry, CodelabModule module) {
        BeanDefinition def = BeanDefinitionBuilder
                .genericBeanDefinition(DataSource.class, () -> CodelabDataSourceFactory.create(environment, module))
                .setDestroyMethodName("close")
                .getBeanDefinition();

        registry.registerBeanDefinition(dataSourceBeanName(module), def);
    }

    static String dataSourceBeanName(CodelabModule module) {
        return module.name() + "DataSource";
    }

    @Override
    public void postProcessBeanFactory(@NonNull ConfigurableListableBeanFactory beanFactory) { /* no-op */ }

    private void registerEntityManagerFactory(
            BeanDefinitionRegistry registry,
            CodelabModule module) {

        BeanDefinition def = BeanDefinitionBuilder
                .genericBeanDefinition(
                        LocalContainerEntityManagerFactoryBean.class,
                        () -> {
                            EntityManagerFactoryBuilder builder =
                                    beanFactory.getBean(EntityManagerFactoryBuilder.class);

                            DataSource dataSource =
                                    beanFactory.getBean(
                                            dataSourceBeanName(module),
                                            DataSource.class);

                            try (Connection connection = dataSource.getConnection()) {
                                DatabaseMetaData metadata = connection.getMetaData();

                                log.info(
                                        "Module {}: JDBC URL={}, DB={}, version={}",
                                        module.persistenceUnit(),
                                        metadata.getURL(),
                                        metadata.getDatabaseProductName(),
                                        metadata.getDatabaseProductVersion()
                                );
                            } catch (SQLException e) {
                                throw new RuntimeException(e);
                            }
                            return builder
                                    .dataSource(dataSource)
                                    .packages(module.entitiesBasePackage())
                                    .persistenceUnit(module.persistenceUnit())
                                    .build();
                        })
                .getBeanDefinition();

        registry.registerBeanDefinition(emfBeanName(module), def);
    }

    static String emfBeanName(CodelabModule module) {
        return module.name() + "EntityManagerFactory";
    }

    private void registerTransactionManager(BeanDefinitionRegistry registry, CodelabModule module) {
        BeanDefinition def = BeanDefinitionBuilder
                .rootBeanDefinition(JpaTransactionManager.class)
                .addPropertyReference("entityManagerFactory", emfBeanName(module))
                .getBeanDefinition();

        registry.registerBeanDefinition(txBeanName(module), def);
    }

    static String txBeanName(CodelabModule module) {
        return module.name() + "TransactionManager";
    }

    private void registerRepositories(BeanDefinitionRegistry registry, CodelabModule module) {
        RepositoryConfigurationSource source = new CodelabJpaRepositoryConfigurationSource(
                module, environment, resourceLoader, registry,
                AnnotationBeanNameGenerator.INSTANCE);
        RepositoryConfigurationExtension extension = new JpaRepositoryConfigExtension();
        RepositoryConfigurationDelegate delegate =
                new RepositoryConfigurationDelegate(source, resourceLoader, environment);
        delegate.registerRepositoriesIn(registry, extension);

    }

}
