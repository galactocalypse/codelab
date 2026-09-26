package com.codelab.orders.entity;

import com.codelab.orders.model.OrderStatus;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import lombok.*;

@Data
@Builder
@Entity
@Table(name = "orders")
@NoArgsConstructor
@AllArgsConstructor
public class OrderEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "customer_id")
  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  private CustomerEntity customer;

  @Column(nullable = false)
  private LocalDate createdAtDate;

  @Column(nullable = false)
  private LocalTime createdAtTime;

  @Column(nullable = false)
  private OrderStatus status;

  @Builder.Default
  @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  private List<LineItemEntity> lineItems = new ArrayList<>();

  @Transient
  public Long getCustomerId() {
    return customer != null ? customer.getId() : null;
  }

  public void addLineItem(ProductEntity product, int count) {
    if (status == OrderStatus.CONFIRMED) {
      throw new IllegalStateException("Cannot modify a shipped order");
    }
    lineItems.stream()
        .filter(li -> li.getProduct().getId().equals(product.getId()))
        .findFirst()
        .ifPresentOrElse(
            existing -> existing.setCount(existing.getCount() + count),
            () -> lineItems.add(new LineItemEntity(this, product, null, count)));
  }

  public void removeLineItem(Long productId) {
    lineItems.removeIf(
        li -> {
          boolean match = li.getProduct().getId().equals(productId);
          if (match) {
            li.setOrder(null); // detach back-reference; orphanRemoval handles the delete
          }
          return match;
        });
  }
}
