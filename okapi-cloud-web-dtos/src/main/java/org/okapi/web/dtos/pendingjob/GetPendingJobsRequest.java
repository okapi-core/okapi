package org.okapi.web.dtos.pendingjob;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public class GetPendingJobsRequest {
  List<String> sources;
}
