package com.codelab.core.utils;

import com.codelab.common.spring.eventbus.CodelabEventConsumer;
import com.google.common.annotations.VisibleForTesting;
import org.springframework.util.Assert;
import org.junit.jupiter.api.Test;

import java.util.Set;

public class CodelabPackageScannerTest {

    @Test
    public void testScan() {
        Set<Class<?>> candidates = CodelabPackageScanner.scan("com.codelab.orders", CodelabEventConsumer.class, false, true);
        Assert.isTrue(!candidates.isEmpty(), "No valid candidates found in package scan");
    }

}
