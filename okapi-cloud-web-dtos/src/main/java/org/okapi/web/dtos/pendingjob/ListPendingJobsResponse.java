package org.okapi.web.dtos.pendingjob;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.okapi.agent.dto.PendingJob;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
public class ListPendingJobsResponse {
  @NotNull
  private List<PendingJob> pendingJobs;
}
