package com.codelab.orders.entity;

import jakarta.persistence.Embeddable;
import java.io.Serializable;
import lombok.Data;

@Data
@Embeddable
public class LineItemId implements Serializable {

  private Long orderId;
  private Long productId;
}
