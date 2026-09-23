package com.codelab.core.jpa;

import com.codelab.common.spring.persistence.CodelabModule;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;
import java.util.LinkedHashMap;
import java.util.Map;

final class CodelabDataSourceFactory {

    private CodelabDataSourceFactory() {}

    static DataSource create(Environment environment, CodelabModule module) {
        String baseUrl = environment.getRequiredProperty("spring.datasource.url");
        String username = environment.getRequiredProperty("spring.datasource.username");
        String password = environment.getRequiredProperty("spring.datasource.password");

        Map<String, String> resolved = new LinkedHashMap<>();
        resolved.put("url", JdbcUrls.withDatabase(baseUrl, module.databaseName()));
        resolved.put("username", username);
        resolved.put("password", password);

        // module-specific overrides win over the shared base (can override url wholesale too)
        resolved.putAll(module.dataSourceOverrides());

        return DataSourceBuilder.create()
                .url(resolved.get("url"))
                .username(resolved.get("username"))
                .password(resolved.get("password"))
                .build();
    }

}
