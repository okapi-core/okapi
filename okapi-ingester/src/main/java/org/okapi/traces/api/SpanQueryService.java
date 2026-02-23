/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.traces.api;

import lombok.RequiredArgsConstructor;
import org.okapi.abstractfilter.AndPageFilter;
import org.okapi.abstractfilter.OrPageFilter;
import org.okapi.abstractfilter.PageFilter;
import org.okapi.exceptions.BadRequestException;
import org.okapi.logs.query.QueryConfig;
import org.okapi.primitives.BinarySpanRecordV2;
import org.okapi.rest.traces.SpanFilterRest;
import org.okapi.rest.traces.SpanQueryRequest;
import org.okapi.rest.traces.SpanQueryResponse;
import org.okapi.traces.io.SpanPageMetadata;
import org.okapi.traces.query.MultiSourceTraceQueryProcessor;
import org.okapi.traces.query.SpanPageTraceFilter;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SpanQueryService {

  MultiSourceTraceQueryProcessor traceQueryProcessor;

  public PageFilter<BinarySpanRecordV2, SpanPageMetadata> createFilter(SpanFilterRest rest)
      throws BadRequestException {
    if (rest.getKind().equals("TRACE")) {
      return new SpanPageTraceFilter(rest.getTraceId());
    } else if (rest.getKind().equals("AND")) {
      return new AndPageFilter<>(createFilter(rest.getLeft()), createFilter(rest.getRight()));
    } else if (rest.getKind().equals("OR")) {
      return new OrPageFilter<>(createFilter(rest.getLeft()), createFilter(rest.getRight()));
    } else throw new BadRequestException("Illegal filter kind: " + rest.getKind());
  }

  public SpanQueryResponse queryAllSources(String app, SpanQueryRequest request, int limit)
      throws Exception {
    if (request.getFilter().getKind() == null) {
      throw new BadRequestException("Filter kind must be specified");
    }
    var filter = createFilter(request.getFilter());
    var queryConfig = new QueryConfig(true, true, true, true);
    var results =
        traceQueryProcessor.getTraces(
            app, request.getStart(), request.getEnd(), filter, queryConfig);
    var dtos = results.stream().limit(limit).map(BinarySpanRecordV2::toSpanDto).toList();
    return new SpanQueryResponse(dtos);
  }

  public SpanQueryResponse queryDiskAndBufferPool(String app, SpanQueryRequest request, int limit)
      throws Exception {
    var queryConfig = new QueryConfig(false, true, true, false);
    var filter = createFilter(request.getFilter());
    var results =
        traceQueryProcessor.getTraces(
            app, request.getStart(), request.getEnd(), filter, queryConfig);
    var dtos = results.stream().limit(limit).map(BinarySpanRecordV2::toSpanDto).toList();
    return new SpanQueryResponse(dtos);
  }
}
