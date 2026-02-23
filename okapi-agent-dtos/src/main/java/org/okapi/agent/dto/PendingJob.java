package org.okapi.agent.dto;

import java.util.Map;
import lombok.*;

@AllArgsConstructor
@Builder
@NoArgsConstructor
@Getter
@Setter
public class PendingJob {
  String jobId;
  String sourceId;
  QuerySpec spec;
  JOB_STATUS jobStatus;
  Map<String, Object> jobParams;
}
