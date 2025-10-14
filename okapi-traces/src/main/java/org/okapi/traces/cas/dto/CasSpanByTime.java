package org.okapi.traces.cas.dto;

import com.datastax.oss.driver.api.mapper.annotations.ClusteringColumn;
import com.datastax.oss.driver.api.mapper.annotations.CqlName;
import com.datastax.oss.driver.api.mapper.annotations.Entity;
import com.datastax.oss.driver.api.mapper.annotations.PartitionKey;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@CqlName("okapi_spans_by_time")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CasSpanByTime {
  @PartitionKey(0)
  String tenantId;

  @PartitionKey(1)
  long bucketSecond;

  @ClusteringColumn(0)
  String statusCode;

  @ClusteringColumn(1)
  long startTimeMs;

  @ClusteringColumn(2)
  String traceId;

  @ClusteringColumn(3)
  String spanId;

  long durationMs;
}
