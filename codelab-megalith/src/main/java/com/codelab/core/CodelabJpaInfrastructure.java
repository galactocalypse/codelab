package com.codelab.core;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.hibernate.autoconfigure.HibernateProperties;
import org.springframework.boot.hibernate.autoconfigure.HibernateSettings;
import org.springframework.boot.jpa.EntityManagerFactoryBuilder;
import org.springframework.boot.jpa.autoconfigure.JpaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({
        JpaProperties.class,
        HibernateProperties.class
})
public class CodelabJpaInfrastructure {

    @Bean
    JpaVendorAdapter jpaVendorAdapter(JpaProperties jpaProperties) {
        HibernateJpaVendorAdapter adapter = new HibernateJpaVendorAdapter();

        adapter.setShowSql(jpaProperties.isShowSql());
        adapter.setGenerateDdl(jpaProperties.isGenerateDdl());

        String platform = jpaProperties.getDatabasePlatform();
        if (platform != null) {
            adapter.setDatabasePlatform(platform);
        }

        return adapter;
    }

    @Bean
    EntityManagerFactoryBuilder entityManagerFactoryBuilder(
            JpaVendorAdapter vendorAdapter,
            JpaProperties jpaProperties,
            HibernateProperties hibernateProperties) {

        return new EntityManagerFactoryBuilder(
                vendorAdapter,
                dataSource -> hibernateProperties.determineHibernateProperties(
                        jpaProperties.getProperties(),
                        new HibernateSettings()
                ),
                null
        );
    }
}
