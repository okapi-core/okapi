package org.okapi.metrics.service;

import com.google.common.collect.Multimap;
import java.util.List;
import org.okapi.rest.metrics.ExportMetricsRequest;

public interface MetricsGrouper {
  Multimap<Integer, ExportMetricsRequest> groupByShard(List<ExportMetricsRequest> exportMetricsRequests);
}
