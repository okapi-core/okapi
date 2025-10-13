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
@CqlName("okapi_traces_by_time")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TraceByTime {
  @PartitionKey(0)
  String tenantId;

  @PartitionKey(1)
  long bucketSecond;

  @ClusteringColumn(0)
  String traceId;

  long firstStartTimeMs;
  long lastEndTimeMs;
  Boolean hasError; // optional
}

