package com.codelab.sample.repository;

import com.codelab.sample.entity.SampleItem;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Per-module repository. Discovered and instantiated by {@code CodelabJpaRegistryUtils} against the
 * module's own {@code samplePU} / {@code sample.transactionManager}.
 */
public interface SampleItemRepository extends JpaRepository<SampleItem, Long> {}
