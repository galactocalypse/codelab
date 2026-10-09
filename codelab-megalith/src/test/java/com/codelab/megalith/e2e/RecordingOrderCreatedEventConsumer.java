package com.codelab.megalith.e2e;

import com.codelab.common.spring.eventbus.BusinessVersion;
import com.codelab.common.spring.eventbus.CodelabEventConsumer;
import com.codelab.common.spring.eventbus.CodelabSubscription;
import com.codelab.orders.model.OrderCreatedEvent;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.pulsar.client.api.SubscriptionInitialPosition;

/**
 * Test observer on the versioned event bus: waits for the {@code OrderCreatedEvent} the normalizer
 * emits from a CDC insert and exposes it to the assertion.
 *
 * <p>{@code Earliest} matters: the event may be produced before this subscription is attached, and
 * the point of the test is that the job→event hop actually happened, not to race the broker.
 */
@CodelabSubscription(
    topic = "created-orders",
    subscriptionName = "e2e-order-created-observer",
    initialPosition = SubscriptionInitialPosition.Earliest,
    concurrency = 1)
public class RecordingOrderCreatedEventConsumer implements CodelabEventConsumer<OrderCreatedEvent> {

  private final CountDownLatch received = new CountDownLatch(1);
  private final AtomicReference<OrderCreatedEvent> event = new AtomicReference<>();

  @Override
  public Set<BusinessVersion> getSupportedBusinessVersions() {
    return Set.of(BusinessVersion.of("v1"));
  }

  @Override
  public void consume(OrderCreatedEvent payload) {
    event.set(payload);
    received.countDown();
  }

  boolean awaitEvent(long timeout, TimeUnit unit) throws InterruptedException {
    return received.await(timeout, unit);
  }

  OrderCreatedEvent event() {
    return event.get();
  }
}
