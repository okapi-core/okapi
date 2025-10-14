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
@CqlName("okapi_spans_by_duration")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CasSpanByDuration {
  @PartitionKey(0)
  String tenantId;

  @PartitionKey(1)
  long bucketSecond;

  @ClusteringColumn(0)
  long durationMs;

  @ClusteringColumn(1)
  String traceId;

  @ClusteringColumn(2)
  String spanId;

  long startTimeMs;
}
