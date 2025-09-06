package org.okapi.metrics.service.web;

import org.okapi.exceptions.BadRequestException;
import org.okapi.rest.metrics.GetMetricsRequestInternal;
import org.okapi.rest.metrics.GetMetricsResponse;
import org.okapi.rest.metrics.SearchMetricsRequestInternal;
import org.okapi.rest.metrics.SearchMetricsResponse;
import org.rocksdb.RocksDBException;

import java.io.IOException;

public interface QueryProcessor {
  GetMetricsResponse getMetricsResponse(GetMetricsRequestInternal request) throws Exception;

  SearchMetricsResponse searchMetricsResponse(SearchMetricsRequestInternal searchMetricsRequest) throws BadRequestException, RocksDBException, IOException;
}
