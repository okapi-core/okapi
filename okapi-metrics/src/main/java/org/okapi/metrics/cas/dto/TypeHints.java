package org.okapi.metrics.cas.dto;

import com.datastax.oss.driver.api.mapper.annotations.CqlName;
import com.datastax.oss.driver.api.mapper.annotations.Entity;
import com.datastax.oss.driver.api.mapper.annotations.PartitionKey;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@CqlName("type_hints")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TypeHints {
  @PartitionKey(0)
  String tenantId;

  @PartitionKey(1)
  String localPath;

  String metricType;
}
