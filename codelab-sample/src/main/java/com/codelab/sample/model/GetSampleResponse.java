package com.codelab.sample.model;

import com.codelab.sample.entity.SampleItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Response returned when reading a sample item. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GetSampleResponse {
  private Long id;
  private String name;

  /** Builds a response from the persisted entity. */
  public static GetSampleResponse from(SampleItem entity) {
    return GetSampleResponse.builder().id(entity.getId()).name(entity.getName()).build();
  }
}
