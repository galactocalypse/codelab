package com.codelab.core.eventbus;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class CodelabTopicRegistry {

  // fully-qualified topic -> owning publisher interface, for collision detection
  private final Map<String, String> topicToOwner = new ConcurrentHashMap<>();

  /**
   * Call this once per candidate during the registrar pass, BEFORE registering its bean definition.
   * Throws on any second interface claiming the same physical topic, whether that's a copy-paste
   * bug within one module or two modules accidentally colliding.
   */
  public void validateNoDuplicateTopic(String resolvedTopic, Class<?> publisherIface) {
    String existingOwner = topicToOwner.putIfAbsent(resolvedTopic, publisherIface.getName());
    if (existingOwner != null && !existingOwner.equals(publisherIface.getName())) {
      throw new IllegalStateException(
          String.format(
              "Topic '%s' is claimed by both %s and %s — each physical topic must have "
                  + "exactly one publisher interface",
              resolvedTopic, existingOwner, publisherIface.getName()));
    }
  }
}
