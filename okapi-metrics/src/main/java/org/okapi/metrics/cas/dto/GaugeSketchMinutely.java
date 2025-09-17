package org.okapi.metrics.cas.dto;

import com.datastax.oss.driver.api.mapper.annotations.ClusteringColumn;
import com.datastax.oss.driver.api.mapper.annotations.CqlName;
import com.datastax.oss.driver.api.mapper.annotations.Entity;
import com.datastax.oss.driver.api.mapper.annotations.PartitionKey;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.nio.ByteBuffer;

@Entity
@CqlName("gauge_sketch_minutely")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GaugeSketchMinutely implements Sketchable{

  @PartitionKey(0)
  String tenantId;

  @PartitionKey(1)
  String localPath;

  @ClusteringColumn(0) long minuteBlock;
  @ClusteringColumn(1) long updateTime;

  ByteBuffer sketch;
}
