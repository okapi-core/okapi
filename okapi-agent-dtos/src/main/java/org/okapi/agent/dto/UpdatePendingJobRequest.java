package org.okapi.agent.dto;

import lombok.*;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
@EqualsAndHashCode
public class UpdatePendingJobRequest {
  String jobId;
  JOB_STATUS status;
}
