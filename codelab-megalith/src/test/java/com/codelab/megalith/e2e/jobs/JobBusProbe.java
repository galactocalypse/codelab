package com.codelab.megalith.e2e.jobs;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Payload for the job-bus E2E — no business version, mirroring real job payloads. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobBusProbe {

  private String value;
}
