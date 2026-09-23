package com.codelab.common.spring.persistence;

import java.util.Map;

public record CodelabModule(
    String name,
    String basePackage,
    String persistenceUnit,
    String databaseName,
    Map<String, String> dataSourceOverrides) {}
