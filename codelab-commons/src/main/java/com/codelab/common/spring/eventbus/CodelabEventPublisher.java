package com.codelab.common.spring.eventbus;

public interface CodelabEventPublisher<T> {

    void publish(T event);

}
