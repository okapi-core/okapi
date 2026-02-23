package org.okapi.logs.service;

import org.okapi.rest.logs.QueryRequest;
import org.okapi.rest.logs.QueryResponse;

public interface LogsQueryService {
  QueryResponse queryAllSources(String streamId, QueryRequest request, int limit)
      throws Exception;

  QueryResponse queryDiskAndBufferPool(
      String streamId, QueryRequest request, int limit) throws Exception;
}
