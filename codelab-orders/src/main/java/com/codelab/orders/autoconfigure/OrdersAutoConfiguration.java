package com.codelab.orders.autoconfigure;

import com.codelab.orders.controller.OrderController;
import com.codelab.orders.workflow.OrderProducer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@Import({OrderController.class, OrderProducer.class})
public class OrdersAutoConfiguration {}
