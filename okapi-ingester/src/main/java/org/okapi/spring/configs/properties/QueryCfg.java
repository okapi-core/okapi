package org.okapi.spring.configs.properties;

import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.okapi.spring.configs.Profiles;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Data
@NoArgsConstructor
@Profile(Profiles.PROFILE_OKAPI_ENGINE)
@AllArgsConstructor
@ConfigurationProperties(prefix = "okapi.query")
@Component
@Validated // ensures startup fail-fast validation
public class QueryCfg {

  @Min(value = 1, message = "logsQueryProcPoolSize must be > 0")
  int logsQueryProcPoolSize;

  @Min(value = 1, message = "metricsQueryProcPoolSize must be > 0")
  int metricsQueryProcPoolSize;

  @Min(value = 1, message = "tracesQueryProcPoolSize must be > 0")
  int tracesQueryProcPoolSize;

  // fanout pools
  @Min(value = 1, message = "logsFanoutPoolSize must be > 0")
  int logsFanoutPoolSize;

  @Min(value = 1, message = "metricsFanoutPoolSize must be > 0")
  int metricsFanoutPoolSize;

  @Min(value = 1, message = "tracesFanoutPoolSize must be > 0")
  int tracesFanoutPoolSize;

  // fanout timeouts
  @Min(value = 100, message = "logsFanoutTimeoutMs must be ≥ 100ms")
  int logsFanoutTimeoutMs;

  @Min(value = 100, message = "metricsFanoutTimeoutMs must be ≥ 100ms")
  int metricsFanoutTimeoutMs;

  @Min(value = 100, message = "tracesFanoutTimeoutMs must be ≥ 100ms")
  int tracesFanoutTimeoutMs;
}
