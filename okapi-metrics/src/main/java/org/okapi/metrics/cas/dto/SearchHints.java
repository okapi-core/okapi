package org.okapi.metrics.cas.dto;

import com.datastax.oss.driver.api.mapper.annotations.ClusteringColumn;
import com.datastax.oss.driver.api.mapper.annotations.CqlName;
import com.datastax.oss.driver.api.mapper.annotations.Entity;
import com.datastax.oss.driver.api.mapper.annotations.PartitionKey;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@CqlName("search_hints")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchHints {
  @PartitionKey(0)
  String tenantId;

  @ClusteringColumn long startMinute;

  String localPath;

  String metricType;
}
