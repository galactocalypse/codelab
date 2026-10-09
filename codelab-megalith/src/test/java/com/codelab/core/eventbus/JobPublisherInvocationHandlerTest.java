package com.codelab.core.eventbus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Proxy;
import java.lang.reflect.UndeclaredThrowableException;
import org.apache.pulsar.client.api.Producer;
import org.apache.pulsar.client.api.PulsarClientException;
import org.apache.pulsar.client.api.Schema;
import org.apache.pulsar.client.api.TypedMessageBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JobPublisherInvocationHandlerTest {

  private static final String TOPIC = "persistent://codelab-movies/local/job.pending-movies";
  private static final long MAX_BYTES = 1024;

  @SuppressWarnings("unchecked")
  private Producer<String> producer;

  @SuppressWarnings("unchecked")
  private TypedMessageBuilder<String> builder;

  private JobPublisherInvocationHandler<String> handler;

  private StringJobPublisher proxy;

  interface StringJobPublisher {
    void publish(String job);

    void publish(String key, String job);

    String ping();
  }

  @BeforeEach
  @SuppressWarnings("unchecked")
  void setUp() throws Exception {
    producer = mock(Producer.class);
    builder = mock(TypedMessageBuilder.class);
    when(producer.newMessage()).thenReturn(builder);
    when(builder.key(any())).thenReturn(builder);
    when(builder.value(any())).thenReturn(builder);
    when(builder.send()).thenReturn(null);

    handler =
        new JobPublisherInvocationHandler<>(
            producer, String.class, Schema.JSON(String.class), TOPIC, MAX_BYTES);
    proxy =
        (StringJobPublisher)
            Proxy.newProxyInstance(
                JobPublisherInvocationHandlerTest.class.getClassLoader(),
                new Class<?>[] {StringJobPublisher.class},
                handler);
  }

  @Test
  void publishesWithoutAnyBusinessVersionProperty() throws Exception {
    proxy.publish("job-payload");

    // The whole point of the job path: no BUSINESS_VERSION stamp, no key when none was given.
    verify(builder, never()).property(any(), any());
    verify(builder, never()).key(any());
    verify(builder).value("job-payload");
    verify(builder).send();
  }

  @Test
  void stampsTheKeyOnKeyedPublish() throws Exception {
    proxy.publish("movie-42", "job-payload");

    verify(builder, never()).property(any(), any());
    verify(builder).key("movie-42");
    verify(builder).value("job-payload");
    verify(builder).send();
  }

  @Test
  void rejectsPayloadsOfTheWrongType() throws Throwable {
    java.lang.reflect.Method publish =
        StringJobPublisher.class.getMethod("publish", String.class, String.class);

    assertThatThrownBy(() -> handler.invoke(proxy, publish, new Object[] {"key", 7}))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Expected");
  }

  @Test
  void rejectsPayloadsExceedingTheJobSizeCap() {
    JobPublisherInvocationHandler<String> tinyCap =
        new JobPublisherInvocationHandler<>(
            producer, String.class, Schema.JSON(String.class), TOPIC, 4);

    StringJobPublisher cappedProxy =
        (StringJobPublisher)
            Proxy.newProxyInstance(
                JobPublisherInvocationHandlerTest.class.getClassLoader(),
                new Class<?>[] {StringJobPublisher.class},
                tinyCap);

    assertThatThrownBy(() -> cappedProxy.publish("hello"))
        .isInstanceOf(CodelabMessageSizeExceededException.class)
        .hasMessageContaining("Job payload for topic " + TOPIC);

    verify(producer, never()).newMessage();
  }

  @Test
  void handlesObjectMethodsOnTheProxy() {
    assertThat(proxy.toString()).contains("JobPublisherInvocationHandler");
    assertThat(proxy.hashCode()).isEqualTo(proxy.hashCode());
    assertThat(proxy.equals(new Object())).isFalse();
    verify(producer, never()).newMessage();
  }

  @Test
  void rejectsUnexpectedMethods() {
    assertThatThrownBy(proxy::ping)
        .isInstanceOf(UnsupportedOperationException.class)
        .hasMessageContaining("Unexpected method on job publisher proxy");
  }

  @Test
  void propagatesSendFailures() throws Exception {
    when(builder.send()).thenThrow(new PulsarClientException.AlreadyClosedException("closed"));

    assertThatThrownBy(() -> proxy.publish("job-payload"))
        .isInstanceOf(UndeclaredThrowableException.class)
        .hasRootCauseInstanceOf(PulsarClientException.AlreadyClosedException.class);
    verify(builder).send();
  }
}
