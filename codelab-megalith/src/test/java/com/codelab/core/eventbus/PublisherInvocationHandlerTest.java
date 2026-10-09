package com.codelab.core.eventbus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.codelab.common.spring.eventbus.BusinessVersion;
import com.codelab.common.spring.eventbus.CodelabMessageProperties;
import java.lang.reflect.Proxy;
import java.lang.reflect.UndeclaredThrowableException;
import org.apache.pulsar.client.api.Producer;
import org.apache.pulsar.client.api.PulsarClientException;
import org.apache.pulsar.client.api.Schema;
import org.apache.pulsar.client.api.TypedMessageBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PublisherInvocationHandlerTest {

  private static final String TOPIC = "persistent://codelab-orders/local/event.created-orders";
  private static final long MAX_BYTES = 1024;

  @SuppressWarnings("unchecked")
  private Producer<String> producer;

  @SuppressWarnings("unchecked")
  private TypedMessageBuilder<String> builder;

  private PublisherInvocationHandler<String> handler;

  private StringPublisher proxy;

  interface StringPublisher {
    void publish(BusinessVersion version, String event);

    void publish(String key, BusinessVersion version, String event);

    String ping();
  }

  @BeforeEach
  @SuppressWarnings("unchecked")
  void setUp() throws Exception {
    producer = mock(Producer.class);
    builder = mock(TypedMessageBuilder.class);
    when(producer.newMessage()).thenReturn(builder);
    when(builder.property(any(), any())).thenReturn(builder);
    when(builder.key(any())).thenReturn(builder);
    when(builder.value(any())).thenReturn(builder);
    when(builder.send()).thenReturn(null);

    handler =
        new PublisherInvocationHandler<>(
            producer, String.class, Schema.JSON(String.class), TOPIC, MAX_BYTES);
    proxy =
        (StringPublisher)
            Proxy.newProxyInstance(
                PublisherInvocationHandlerTest.class.getClassLoader(),
                new Class<?>[] {StringPublisher.class},
                handler);
  }

  @Test
  void stampsTheBusinessVersionPropertyOnVersionedPublish() throws Exception {
    proxy.publish(BusinessVersion.of("v2"), "hello");

    verify(builder, never()).key(any());
    verify(builder).property(CodelabMessageProperties.BUSINESS_VERSION, "v2");
    verify(builder).value("hello");
    verify(builder).send();
  }

  @Test
  void stampsTheVersionAndKeyOnKeyedPublish() throws Exception {
    proxy.publish("order-1", BusinessVersion.of("v2"), "hello");

    verify(builder).property(CodelabMessageProperties.BUSINESS_VERSION, "v2");
    verify(builder).key("order-1");
    verify(builder).value("hello");
    verify(builder).send();
  }

  @Test
  void rejectsPayloadsOfTheWrongType() throws Throwable {
    // The JDK proxy already rejects mismatched arguments, so exercise the handler's
    // own payloadType.isInstance guard directly — defense in depth, not the only net.
    java.lang.reflect.Method publish =
        StringPublisher.class.getMethod("publish", BusinessVersion.class, String.class);

    assertThatThrownBy(
            () -> handler.invoke(proxy, publish, new Object[] {BusinessVersion.of("v1"), 7}))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Expected");
  }

  @Test
  void rejectsPayloadsExceedingTheEventSizeCap() {
    PublisherInvocationHandler<String> tinyCap =
        new PublisherInvocationHandler<>(
            producer, String.class, Schema.JSON(String.class), TOPIC, 4);

    StringPublisher cappedProxy =
        (StringPublisher)
            Proxy.newProxyInstance(
                PublisherInvocationHandlerTest.class.getClassLoader(),
                new Class<?>[] {StringPublisher.class},
                tinyCap);

    assertThatThrownBy(() -> cappedProxy.publish(BusinessVersion.of("v1"), "hello"))
        .isInstanceOf(CodelabMessageSizeExceededException.class)
        .hasMessageContaining("Event payload for topic " + TOPIC);

    // Rejected before the producer is touched — the oversized message never reaches the broker.
    verify(producer, never()).newMessage();
  }

  @Test
  void handlesObjectMethodsOnTheProxy() {
    // Object methods go through invoke() too; they must not touch the producer.
    assertThat(proxy.toString()).contains("PublisherInvocationHandler");
    assertThat(proxy.hashCode()).isEqualTo(proxy.hashCode());
    assertThat(proxy.equals(new Object())).isFalse();
    verify(producer, never()).newMessage();
  }

  @Test
  void rejectsUnexpectedMethods() {
    assertThatThrownBy(proxy::ping)
        .isInstanceOf(UnsupportedOperationException.class)
        .hasMessageContaining("Unexpected method");
  }

  @Test
  void propagatesSendFailures() throws Exception {
    when(builder.send()).thenThrow(new PulsarClientException.AlreadyClosedException("closed"));

    // publish() declares no checked exceptions, so the JDK proxy wraps the
    // checked PulsarClientException in an UndeclaredThrowableException.
    assertThatThrownBy(() -> proxy.publish(BusinessVersion.of("v1"), "hello"))
        .isInstanceOf(UndeclaredThrowableException.class)
        .hasRootCauseInstanceOf(PulsarClientException.AlreadyClosedException.class);
    verify(builder).send();
  }
}
