package org.okapi.metrics.service.web;

import java.io.IOException;
import org.okapi.exceptions.BadRequestException;
import org.okapi.rest.metrics.query.GetMetricsRequest;
import org.okapi.rest.metrics.query.GetMetricsResponse;
import org.okapi.rest.metrics.query.ListMetricsRequest;
import org.okapi.rest.metrics.query.ListMetricsResponse;
import org.okapi.rest.metrics.search.SearchMetricsRequestInternal;
import org.okapi.rest.metrics.search.SearchMetricsResponse;
import org.rocksdb.RocksDBException;

public interface QueryProcessor {
  GetMetricsResponse getMetricsResponse(GetMetricsRequest request) throws Exception;

  SearchMetricsResponse searchMetricsResponse(SearchMetricsRequestInternal searchMetricsRequest) throws BadRequestException, RocksDBException, IOException;

  ListMetricsResponse listMetricsResponse(ListMetricsRequest request);
}
