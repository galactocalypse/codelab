package com.codelab.sample.publisher;

import com.codelab.common.spring.eventbus.CodelabEventPublisher;
import com.codelab.common.spring.eventbus.CodelabTopic;
import com.codelab.sample.model.SampleCreatedEvent;

/**
 * Per-type producer for {@link SampleCreatedEvent}s - the event-bus analogue of a JPA repository.
 * The publisher proxy is registered by {@code CodelabPulsarRegistryUtils} from the
 * {@code @CodelabTopic} annotation; inject it wherever a publish is needed.
 */
@CodelabTopic("created-samples")
public interface SampleCreatedEventPublisher extends CodelabEventPublisher<SampleCreatedEvent> {}
