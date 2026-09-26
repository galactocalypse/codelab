package com.codelab.orders.model;

import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class CreateOrderRequest {

  private Long customerId;
  private List<LineItem> lineItems;

  @Data
  @NoArgsConstructor
  public static class LineItem {
    private Long productId;
    private int count;
  }
}
