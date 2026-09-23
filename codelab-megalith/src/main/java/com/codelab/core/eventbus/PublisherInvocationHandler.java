package com.codelab.core.eventbus;

import org.apache.pulsar.client.api.Producer;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

class PublisherInvocationHandler implements InvocationHandler {

    private final Producer<Object> producer;

    PublisherInvocationHandler(Producer<Object> producer) {
        this.producer = producer;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // Object-class methods (equals/hashCode/toString) hit the proxy itself,
        // not your single business method — handle them or every log statement
        // touching this bean will throw
        if (method.getDeclaringClass() == Object.class) {
            return method.invoke(this, args);
        }

        if ("publish".equals(method.getName()) && args.length == 1) {
            return producer.send(args[0]); // or sendAsync(), depending on your sync/async decision
        }

        throw new UnsupportedOperationException(
                "Unexpected method on publisher proxy: " + method);
    }
}