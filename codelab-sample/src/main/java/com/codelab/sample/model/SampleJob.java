package com.codelab.sample.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Async unit of work on the {@code pending-samples} job bus. Job payloads carry no business version
 * - the contract is owned end-to-end by the producer ({@code SampleFeedTask}) and consumer ({@link
 * com.codelab.sample.worker.SampleJobProcessor}).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SampleJob {
  private Long sampleId;
}
