package com.codelab.sample.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Request body for creating a sample item. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateSampleRequest {
  private String name;
}
