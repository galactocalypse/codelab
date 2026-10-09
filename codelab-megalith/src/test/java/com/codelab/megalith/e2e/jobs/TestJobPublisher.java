package com.codelab.megalith.e2e.jobs;

import com.codelab.common.spring.jobbus.CodelabJobPublisher;
import com.codelab.common.spring.jobbus.CodelabJobTopic;

/** Test job publisher, resolved by the same scanner production modules use. */
@CodelabJobTopic("test-work")
public interface TestJobPublisher extends CodelabJobPublisher<JobBusProbe> {}
