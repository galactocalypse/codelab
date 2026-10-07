package com.codelab.movies.tmdb.queue;

import com.codelab.common.spring.eventbus.CodelabEventPublisher;
import com.codelab.common.spring.eventbus.CodelabTopic;
import com.codelab.movies.tmdb.parser.MovieEvent;

@CodelabTopic("pending-movies")
public interface MovieEventPublisher extends CodelabEventPublisher<MovieEvent> {}
