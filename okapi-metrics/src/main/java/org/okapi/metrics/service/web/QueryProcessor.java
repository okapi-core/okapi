package org.okapi.metrics.service.web;

import java.io.IOException;
import org.okapi.exceptions.BadRequestException;
import org.okapi.rest.metrics.*;
import org.rocksdb.RocksDBException;

public interface QueryProcessor {
  GetMetricsResponse getMetricsResponse(GetMetricsRequestInternal request) throws Exception;

  SearchMetricsResponse searchMetricsResponse(SearchMetricsRequestInternal searchMetricsRequest) throws BadRequestException, RocksDBException, IOException;

  ListMetricsResponse listMetricsResponse(ListMetricsRequest request);
}
