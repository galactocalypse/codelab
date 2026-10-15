package com.codelab.sample.model;

import com.codelab.sample.entity.SampleItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Response returned after creating a sample item. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateSampleResponse {
  private Long id;

  /** Builds a response from the persisted entity. */
  public static CreateSampleResponse from(SampleItem entity) {
    return CreateSampleResponse.builder().id(entity.getId()).build();
  }
}
