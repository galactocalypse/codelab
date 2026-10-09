package com.codelab.core.eventbus;

/**
 * Thrown when a message cannot be forwarded to the DLQ; the message is left unacked and retried.
 */
public class CodelabDlqRoutingException extends RuntimeException {

  public CodelabDlqRoutingException(String message) {
    super(message);
  }

  public CodelabDlqRoutingException(String message, Throwable cause) {
    super(message, cause);
  }
}
