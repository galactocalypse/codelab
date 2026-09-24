package com.codelab.core.utils;

import com.codelab.common.spring.eventbus.CodelabEventConsumer;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.util.Assert;

public class CodelabPackageScannerTest {

  @Test
  public void testScan() {
    Set<Class<?>> candidates =
        CodelabPackageScanner.scan("com.codelab.orders", CodelabEventConsumer.class, false, true);
    Assert.isTrue(!candidates.isEmpty(), "No valid candidates found in package scan");
  }
}
