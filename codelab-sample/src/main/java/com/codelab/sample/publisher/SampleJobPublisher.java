package com.codelab.sample.publisher;

import com.codelab.common.spring.jobbus.CodelabJobPublisher;
import com.codelab.common.spring.jobbus.CodelabJobTopic;
import com.codelab.sample.model.SampleJob;

/**
 * Per-type producer for {@link SampleJob}s on the job bus. Job producers carry no business version;
 * failure handling is Pulsar-native (nack -> redelivery -> DLQ).
 */
@CodelabJobTopic("pending-samples")
public interface SampleJobPublisher extends CodelabJobPublisher<SampleJob> {}
