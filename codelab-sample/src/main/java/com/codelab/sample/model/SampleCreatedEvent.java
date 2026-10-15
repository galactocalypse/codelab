package com.codelab.sample.model;

import com.codelab.sample.entity.SampleItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.pulsar.common.schema.SchemaType;
import org.springframework.pulsar.annotation.PulsarMessage;

/**
 * Business fact emitted when a sample item is created. Published by {@link
 * com.codelab.sample.service.SampleServiceImpl} with an explicit {@link
 * com.codelab.common.spring.eventbus.BusinessVersion}. The schema type is JSON so the Pulsar
 * message can be decoded on the consumer side.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@PulsarMessage(schemaType = SchemaType.JSON)
public class SampleCreatedEvent {

  private Long sampleId;

  /** Builds an event payload from the persisted entity. */
  public static SampleCreatedEvent from(SampleItem item) {
    return SampleCreatedEvent.builder().sampleId(item.getId()).build();
  }
}
