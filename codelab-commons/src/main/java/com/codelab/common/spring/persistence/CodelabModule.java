package com.codelab.common.spring.persistence;


import java.util.Map;

public record CodelabModule(String name, String persistenceUnit, String entitiesBasePackage,
                            String repositoriesBasePackage, String databaseName,
                            Map<String, String> dataSourceOverrides) {

    public CodelabModule(String name, String persistenceUnit, String entitiesBasePackage, String repositoriesBasePackage, String databaseName) {
        this(name, persistenceUnit, entitiesBasePackage, repositoriesBasePackage, databaseName, Map.of());
    }

}
