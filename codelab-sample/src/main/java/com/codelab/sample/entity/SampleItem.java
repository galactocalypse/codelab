package com.codelab.sample.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Tiny aggregate used by the sample module. Kept intentionally small: the point of this module is
 * to showcase megalith wiring, not domain modelling.
 */
@Data
@Entity
@Builder
@Table(name = "sample_items")
@NoArgsConstructor
@AllArgsConstructor
public class SampleItem {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String name;
}
