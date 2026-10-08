package com.codelab.common.spring.eventbus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class BusinessVersionTest {

  @Test
  void acceptsTokensFromTheAllowedFormat() {
    assertEquals("v1", BusinessVersion.of("v1").value());
    assertEquals("v", BusinessVersion.of("v").value());
    assertEquals("2025.4.2", BusinessVersion.of("2025.4.2").value());
    assertEquals("my_event-2", BusinessVersion.of("  my_event-2  ").value());
    assertEquals("A.b_c-d1", BusinessVersion.of("A.b_c-d1").value());
  }

  @Test
  void rejectsInvalidTokens() {
    assertThrows(IllegalArgumentException.class, () -> BusinessVersion.of("-v1"));
    assertThrows(IllegalArgumentException.class, () -> BusinessVersion.of(".start"));
    assertThrows(IllegalArgumentException.class, () -> BusinessVersion.of("v1!"));
    assertThrows(IllegalArgumentException.class, () -> BusinessVersion.of("v1 v2"));
    assertThrows(IllegalArgumentException.class, () -> BusinessVersion.of(""));
    assertThrows(IllegalArgumentException.class, () -> BusinessVersion.of("   "));
    assertThrows(IllegalArgumentException.class, () -> BusinessVersion.of(null));
  }

  @Test
  void matchesByExactOpaqueValue() {
    assertEquals(BusinessVersion.of("v1"), BusinessVersion.of(" v1 "));
    assertNotEquals(BusinessVersion.of("v1"), BusinessVersion.of("V1"));
    assertNotEquals(BusinessVersion.of("v1"), BusinessVersion.of("v2"));
  }

  @Test
  void exposesTheLegacySentinel() {
    assertEquals("legacy", BusinessVersion.LEGACY.value());
    assertEquals(BusinessVersion.LEGACY, BusinessVersion.of("legacy"));
  }
}
