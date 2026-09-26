package com.codelab.orders.entity;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Entity
@Table(name = "line_items")
@NoArgsConstructor
@AllArgsConstructor
public class LineItemEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @MapsId("orderId")
  @JoinColumn(name = "order_id")
  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  private OrderEntity order;

  @ManyToOne(fetch = FetchType.LAZY)
  @MapsId("productId")
  @JoinColumn(name = "product_id")
  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  private ProductEntity product;

  @EmbeddedId private LineItemId id;

  @Column(nullable = false)
  private int count;

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof LineItemEntity that)) return false;
    return id != null && id.equals(that.id);
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }
}
