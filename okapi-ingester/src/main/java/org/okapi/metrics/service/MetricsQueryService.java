/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.metrics.service;

import java.io.IOException;
import org.okapi.exceptions.BadRequestException;
import org.okapi.logs.query.QueryConfig;
import org.okapi.rest.metrics.query.GetMetricsRequest;
import org.okapi.rest.metrics.query.GetMetricsResponse;
import org.okapi.rest.metrics.search.SearchMetricsRequestInternal;
import org.okapi.rest.metrics.search.SearchMetricsResponse;

public interface MetricsQueryService {
  GetMetricsResponse getMetricsResponse(GetMetricsRequest request, QueryConfig queryConfig)
      throws Exception;

  SearchMetricsResponse searchMetricsResponse(
      SearchMetricsRequestInternal searchMetricsRequest, QueryConfig config)
      throws BadRequestException, IOException;
}
