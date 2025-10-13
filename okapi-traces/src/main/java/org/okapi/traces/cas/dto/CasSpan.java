package org.okapi.traces.cas.dto;

import com.datastax.oss.driver.api.mapper.annotations.ClusteringColumn;
import com.datastax.oss.driver.api.mapper.annotations.CqlName;
import com.datastax.oss.driver.api.mapper.annotations.Entity;
import com.datastax.oss.driver.api.mapper.annotations.PartitionKey;
import java.nio.ByteBuffer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@CqlName("okapi_spans")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CasSpan {

  @PartitionKey(0)
  String tenantId;

  @PartitionKey(1)
  String traceId;

  @ClusteringColumn(0)
  String spanId;

  String parentSpanId;
  String name;
  long startTimeMs;
  long endTimeMs;
  long durationMs;
  String kind;
  String statusCode;
  String statusMessage;

  ByteBuffer attributesBlob;
  ByteBuffer eventsBlob;
}

