package com.codelab.sample.autoconfigure;

import com.codelab.common.spring.persistence.CodelabModule;
import com.codelab.common.spring.persistence.CodelabModuleProvider;
import java.util.Map;

/**
 * Declares this module to the megalith. The {@code basePackage} drives the per-module scan for JPA
 * entities/repositories, Pulsar publishers/subscriptions and {@code CodelabTask} descriptors. The
 * {@code databaseName} selects the logical database ({@code sample}) on the shared Postgres server,
 * exactly like {@code movies} and {@code orders} target their own databases.
 */
public class SampleModuleProvider implements CodelabModuleProvider {

  @Override
  public CodelabModule provide() {
    return new CodelabModule("sample", "com.codelab.sample", "samplePU", "sample", Map.of());
  }
}
