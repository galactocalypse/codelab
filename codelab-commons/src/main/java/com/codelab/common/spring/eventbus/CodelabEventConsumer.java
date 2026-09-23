package com.codelab.common.spring.eventbus;

public interface CodelabEventConsumer<T> {

    void consume(T event);

}
