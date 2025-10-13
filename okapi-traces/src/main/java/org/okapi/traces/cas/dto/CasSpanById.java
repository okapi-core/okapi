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
@CqlName("okapi_spans_by_id")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CasSpanById {
  @PartitionKey(0)
  String tenantId;

  @ClusteringColumn(0)
  String spanId;

  String traceId;
}

