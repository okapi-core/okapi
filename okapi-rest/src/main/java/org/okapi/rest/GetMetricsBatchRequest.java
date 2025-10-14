package org.okapi.rest;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Builder
@AllArgsConstructor
public class GetMetricsBatchRequest {
  @Getter String teamsId;
  @Getter String appId; // UUID
  @Getter String hostId; // UUID
  @Getter String metricName;
  @Getter String path;
  @Getter Map<String, String> tags;
  @Getter String type;
  @Getter long startTime;
  @Getter long endTime;

  @Getter String resolution;

  @Getter String aggregation;
}
