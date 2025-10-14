package org.okapi.metrics.cas.dto;

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
@CqlName("gauge_sketch_hourly")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GaugeSketchHourly implements Sketchable {

  @PartitionKey(0)
  String tenantId;

  @PartitionKey(1)
  String localPath;

  @ClusteringColumn(0)
  long hrBlock;

  @ClusteringColumn(1)
  long updateTime;

  ByteBuffer sketch;
}
