package com.codelab.sample.service;

import com.codelab.common.spring.eventbus.BusinessVersion;
import com.codelab.sample.entity.SampleItem;
import com.codelab.sample.model.CreateSampleRequest;
import com.codelab.sample.model.CreateSampleResponse;
import com.codelab.sample.model.GetSampleResponse;
import com.codelab.sample.model.SampleCreatedEvent;
import com.codelab.sample.model.SampleJob;
import com.codelab.sample.publisher.SampleCreatedEventPublisher;
import com.codelab.sample.publisher.SampleJobPublisher;
import com.codelab.sample.repository.SampleItemRepository;
import lombok.AllArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

/** Default implementation of {@link SampleService}. */
@AllArgsConstructor
public class SampleServiceImpl implements SampleService {

  private final SampleItemRepository repository;
  private final SampleCreatedEventPublisher createdEventPublisher;
  private final SampleJobPublisher jobPublisher;

  @Override
  @Transactional(transactionManager = "sample.transactionManager")
  public CreateSampleResponse createSample(CreateSampleRequest request) {
    SampleItem saved = repository.save(SampleItem.builder().name(request.getName()).build());
    // The business version is mandated by the framework and stamped on the message.
    createdEventPublisher.publish(BusinessVersion.of("v1"), SampleCreatedEvent.from(saved));
    return CreateSampleResponse.from(saved);
  }

  @Override
  @Transactional(transactionManager = "sample.transactionManager", readOnly = true)
  public GetSampleResponse getSample(Long id) {
    SampleItem item =
        repository
            .findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Sample item " + id + " not found"));
    return GetSampleResponse.from(item);
  }

  @Override
  public void submitForProcessing(Long id) {
    // Jobs carry no business version; failure handling is Pulsar-native.
    jobPublisher.publish(new SampleJob(id));
  }
}
