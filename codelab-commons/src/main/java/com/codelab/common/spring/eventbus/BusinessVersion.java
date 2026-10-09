package com.codelab.common.spring.eventbus;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * The <em>business</em> version of a Codelab message or event, carried as the {@link
 * CodelabMessageProperties#BUSINESS_VERSION} message property. It is independent of the schema
 * version ({@code schemaVersion}) Pulsar maintains for the topic schema: business version captures
 * the contract version of the event, schema version captures the wire-schema evolution.
 *
 * <p>Semantics are exact, opaque string match — a consumer either supports the exact version or it
 * does not. Versions are validated to a safe, non-blank token format: {@code
 * [A-Za-z0-9][A-Za-z0-9._-]{0,63}}.
 */
public final class BusinessVersion {

  private static final Pattern FORMAT = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]{0,63}");

  /** Sentinel used for messages that carry no {@link CodelabMessageProperties#BUSINESS_VERSION}. */
  public static final BusinessVersion LEGACY = of("legacy");

  private final String value;

  private BusinessVersion(String value) {
    this.value = value;
  }

  /** Parses and validates a business version, trimming surrounding whitespace. */
  public static BusinessVersion of(String value) {
    if (value == null) {
      throw new IllegalArgumentException("Business version must not be null");
    }
    String normalized = value.trim();
    if (!FORMAT.matcher(normalized).matches()) {
      throw new IllegalArgumentException(
          "Invalid business version '" + value + "' — expected [A-Za-z0-9][A-Za-z0-9._-]{0,63}");
    }
    return new BusinessVersion(normalized);
  }

  public String value() {
    return value;
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof BusinessVersion other && value.equals(other.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(value);
  }

  @Override
  public String toString() {
    return value;
  }
}
