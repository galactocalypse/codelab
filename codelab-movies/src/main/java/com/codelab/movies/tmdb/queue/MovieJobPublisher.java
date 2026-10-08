package com.codelab.movies.tmdb.queue;

import com.codelab.common.spring.jobbus.CodelabJobPublisher;
import com.codelab.common.spring.jobbus.CodelabJobTopic;

@CodelabJobTopic("pending-movies")
public interface MovieJobPublisher extends CodelabJobPublisher<MovieJob> {}
