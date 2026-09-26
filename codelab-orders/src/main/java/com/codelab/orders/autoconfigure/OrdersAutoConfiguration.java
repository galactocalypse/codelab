package com.codelab.orders.autoconfigure;

import com.codelab.orders.controller.CustomerController;
import com.codelab.orders.controller.OrderController;
import com.codelab.orders.controller.ProductController;
import com.codelab.orders.service.CustomerServiceImpl;
import com.codelab.orders.service.OrderService;
import com.codelab.orders.service.OrderServiceImpl;
import com.codelab.orders.service.ProductServiceImpl;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@Import({OrderController.class, ProductController.class, CustomerController.class,
        OrderServiceImpl.class, CustomerServiceImpl.class, ProductServiceImpl.class})
public class OrdersAutoConfiguration {}
