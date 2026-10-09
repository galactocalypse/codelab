package com.codelab.orders.cdc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Debezium change-event envelope for the {@code orders} table, as delivered by Debezium Server onto
 * the {@code orders-cdc.public.orders} job topic. Only the fields the normalizer needs are modeled;
 * everything else in the envelope ({@code source} block, {@code ts_ms}, ...) is ignored.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CdcOrderChange {

  /**
   * Debezium operation code: {@code c} insert, {@code u} update, {@code d} delete, {@code r}
   * snapshot.
   */
  private String op;

  private CdcOrderRow before;

  private CdcOrderRow after;
}
