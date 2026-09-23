package com.codelab.orders.autoconfigure;

import com.codelab.common.spring.persistence.CodelabModule;
import com.codelab.common.spring.persistence.CodelabModuleProvider;
import java.util.Map;

public class OrdersModuleProvider implements CodelabModuleProvider {
  @Override
  public CodelabModule provide() {
    return new CodelabModule("orders", "com.codelab.orders", "ordersPU", "orders", Map.of());
  }
}
