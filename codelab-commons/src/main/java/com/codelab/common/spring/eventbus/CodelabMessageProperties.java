package com.codelab.common.spring.eventbus;

/** Property keys the Codelab event bus uses on Pulsar messages. */
public final class CodelabMessageProperties {

  /** The business version of the event, stamped by the publisher at publish time. */
  public static final String BUSINESS_VERSION = "codelab-business-version";

  // codelab.dlq.* metadata added by the framework when it routes a message to the DLQ.
  public static final String DLQ_REASON = "codelab.dlq.reason";
  public static final String DLQ_OBSERVED_VERSION = "codelab.dlq.observed.business.version";
  public static final String DLQ_CONSUMER = "codelab.dlq.consumer";
  public static final String DLQ_ORIGINAL_TOPIC = "codelab.dlq.original.topic";
  public static final String DLQ_ORIGINAL_SUBSCRIPTION = "codelab.dlq.original.subscription";
  public static final String DLQ_ORIGINAL_MESSAGE_ID = "codelab.dlq.original.messageId";
  public static final String DLQ_ROUTED_AT = "codelab.dlq.routedAt";

  public static final String DLQ_REASON_UNSUPPORTED_BUSINESS_VERSION =
      "UNSUPPORTED_BUSINESS_VERSION";
  public static final String DLQ_REASON_MISSING_BUSINESS_VERSION = "MISSING_BUSINESS_VERSION";

  private CodelabMessageProperties() {}
}
