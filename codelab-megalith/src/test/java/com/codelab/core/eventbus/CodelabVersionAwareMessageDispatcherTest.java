package com.codelab.core.eventbus;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.codelab.common.spring.eventbus.BusinessVersion;
import com.codelab.common.spring.eventbus.CodelabEventConsumer;
import com.codelab.common.spring.eventbus.CodelabMessageProperties;
import java.util.Map;
import java.util.Set;
import org.apache.pulsar.client.api.Message;
import org.apache.pulsar.client.api.MessageId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CodelabVersionAwareMessageDispatcherTest {

  private static final String DLQ = "movies-events-created-movie-logger-DLQ";
  private static final byte[] JSON = "{\"id\":\"m-1\"}".getBytes();

  private CodelabDlqRouter dlqRouter;
  private CodelabEventConsumer<Payload> delegate;
  private CodelabVersionAwareMessageDispatcher<Payload> dispatcher;
  private Message<byte[]> message;

  record Payload(String id) {}

  @BeforeEach
  @SuppressWarnings("unchecked")
  void setUp() {
    dlqRouter = mock(CodelabDlqRouter.class);
    delegate = mock(CodelabEventConsumer.class);
    message = mock(Message.class);
    when(message.getData()).thenReturn(JSON);
    when(message.getTopicName()).thenReturn("persistent://codelab/movies/movies-events");
    when(message.getMessageId()).thenReturn(mock(MessageId.class));
  }

  private void withSupported(BusinessVersion... versions) throws Exception {
    when(delegate.getSupportedBusinessVersions()).thenReturn(Set.of(versions));
    dispatcher =
        new CodelabVersionAwareMessageDispatcher<>(
            delegate, Payload.class, Set.of(versions), DLQ, "created-movie-logger", dlqRouter);
  }

  private void versionProperty(String value) {
    when(message.getProperty(CodelabMessageProperties.BUSINESS_VERSION)).thenReturn(value);
  }

  @Test
  void decodesAndConsumesSupportedVersions() throws Exception {
    withSupported(BusinessVersion.of("v1"));
    versionProperty("v1");

    dispatcher.dispatch(message);

    verify(delegate).consume(eq(new Payload("m-1")));
    verify(dlqRouter, never()).publishToDeadLetter(any(), any(), any());
  }

  @Test
  void treatsMissingVersionAsLegacyWhenSupported() throws Exception {
    withSupported(BusinessVersion.LEGACY);
    versionProperty(null);

    dispatcher.dispatch(message);

    verify(delegate).consume(eq(new Payload("m-1")));
    verify(dlqRouter, never()).publishToDeadLetter(any(), any(), any());
  }

  @Test
  void routesUnsupportedVersionsToDlqWithoutConsuming() throws Exception {
    withSupported(BusinessVersion.of("v1"));
    versionProperty("v2");

    dispatcher.dispatch(message);

    verify(delegate, never()).consume(any());
    verify(dlqRouter)
        .publishToDeadLetter(
            eq(DLQ),
            eq(message),
            argThat(
                (Map<String, String> metadata) ->
                    CodelabMessageProperties.DLQ_REASON_UNSUPPORTED_BUSINESS_VERSION.equals(
                            metadata.get(CodelabMessageProperties.DLQ_REASON))
                        && "v2".equals(metadata.get(CodelabMessageProperties.DLQ_OBSERVED_VERSION))
                        && "persistent://codelab/movies/movies-events"
                            .equals(metadata.get(CodelabMessageProperties.DLQ_ORIGINAL_TOPIC))
                        && metadata.containsKey(CodelabMessageProperties.DLQ_ORIGINAL_MESSAGE_ID)
                        && metadata.containsKey(CodelabMessageProperties.DLQ_ROUTED_AT)));
  }

  @Test
  void routesMissingVersionToDlqWhenLegacyNotSupported() throws Exception {
    withSupported(BusinessVersion.of("v1"));
    versionProperty(null);

    dispatcher.dispatch(message);

    verify(delegate, never()).consume(any());
    verify(dlqRouter)
        .publishToDeadLetter(
            eq(DLQ),
            eq(message),
            argThat(
                (Map<String, String> metadata) ->
                    CodelabMessageProperties.DLQ_REASON_MISSING_BUSINESS_VERSION.equals(
                            metadata.get(CodelabMessageProperties.DLQ_REASON))
                        && BusinessVersion.LEGACY
                            .value()
                            .equals(metadata.get(CodelabMessageProperties.DLQ_OBSERVED_VERSION))));
  }

  @Test
  void routesMalformedVersionsToDlqAsUnsupported() throws Exception {
    withSupported(BusinessVersion.of("v1"));
    versionProperty("v1!");

    dispatcher.dispatch(message);

    verify(delegate, never()).consume(any());
    verify(dlqRouter)
        .publishToDeadLetter(
            eq(DLQ),
            eq(message),
            argThat(
                (Map<String, String> metadata) ->
                    CodelabMessageProperties.DLQ_REASON_UNSUPPORTED_BUSINESS_VERSION.equals(
                            metadata.get(CodelabMessageProperties.DLQ_REASON))
                        && "v1!"
                            .equals(metadata.get(CodelabMessageProperties.DLQ_OBSERVED_VERSION))));
  }

  @Test
  void propagatesDlqFailuresSoTheMessageIsRetriedNotLost() throws Exception {
    withSupported(BusinessVersion.of("v1"));
    versionProperty("v99");
    doThrow(new CodelabDlqRoutingException("broker down"))
        .when(dlqRouter)
        .publishToDeadLetter(any(), any(), any());

    assertThatThrownBy(() -> dispatcher.dispatch(message))
        .isInstanceOf(CodelabDlqRoutingException.class)
        .hasMessageContaining("broker down");
  }
}
