package com.codelab.orders.autoconfigure;

import com.codelab.orders.controller.CustomerController;
import com.codelab.orders.controller.OrderController;
import com.codelab.orders.controller.ProductController;
import com.codelab.orders.service.CustomerServiceImpl;
import com.codelab.orders.service.OrderServiceImpl;
import com.codelab.orders.service.ProductServiceImpl;
import org.apache.pulsar.client.api.DeadLetterPolicy;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@Import({
  OrderController.class,
  ProductController.class,
  CustomerController.class,
  OrderServiceImpl.class,
  CustomerServiceImpl.class,
  ProductServiceImpl.class
})
public class OrdersAutoConfiguration {

  /**
   * Referenced by {@code OrderCdcNormalizer}'s {@code deadLetterPolicyRef}. Without it, a
   * malformed-but-always-failing CDC envelope would be redelivered forever against {@code
   * ackTimeoutSeconds = 60}.
   */
  @Bean
  public DeadLetterPolicy orderCdcDeadLetterPolicy() {
    return DeadLetterPolicy.builder().maxRedeliverCount(3).build();
  }
}
