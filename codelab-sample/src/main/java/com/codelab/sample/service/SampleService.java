package com.codelab.sample.service;

import com.codelab.sample.model.CreateSampleRequest;
import com.codelab.sample.model.CreateSampleResponse;
import com.codelab.sample.model.GetSampleResponse;

/**
 * Application service for sample items. Demonstrates a per-module transaction manager, the event
 * bus (publish on create) and the job bus (submit for async processing).
 */
public interface SampleService {

  /** Creates and persists a sample item, then publishes a {@code created-samples} event. */
  CreateSampleResponse createSample(CreateSampleRequest request);

  /** Reads a sample item by id. */
  GetSampleResponse getSample(Long id);

  /** Enqueues the item for asynchronous job processing. */
  void submitForProcessing(Long id);
}
