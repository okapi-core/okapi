package org.okapi.common;

import org.okapi.spring.configs.properties.QueryCfg;

public class CommonConfigs {
  public static QueryCfg getQueryCfg() {
    var cfg = new QueryCfg();
    cfg.setLogsQueryProcPoolSize(4);
    cfg.setLogsFanoutPoolSize(2);
    cfg.setMetricsQueryProcPoolSize(4);
    cfg.setMetricsFanoutPoolSize(2);
    cfg.setTracesQueryProcPoolSize(4);
    cfg.setTracesFanoutPoolSize(2);
    return cfg;
  }
}
