package org.okapi.web.dtos.pendingjob;

import lombok.*;
import org.okapi.agent.dto.JOB_STATUS;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
@EqualsAndHashCode
public class UpdatePendingJobRequest {
  String jobId;
  JOB_STATUS status;
}
