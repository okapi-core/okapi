package org.okapi.logs.service;

import org.okapi.abstractfilter.AndPageFilter;
import org.okapi.abstractfilter.OrPageFilter;
import org.okapi.abstractfilter.PageFilter;
import org.okapi.exceptions.BadRequestException;
import org.okapi.logs.io.LogPageMetadata;
import org.okapi.logs.query.*;
import org.okapi.logs.query.processor.MultiSourceLogsQueryProcessor;
import org.okapi.primitives.BinaryLogRecordV1;
import org.okapi.rest.logs.FilterNode;
import org.okapi.rest.logs.QueryRequest;
import org.okapi.rest.logs.QueryResponse;

public class QueryExecutorServiceImpl implements LogsQueryService {

  private final MultiSourceLogsQueryProcessor processor;

  public QueryExecutorServiceImpl(MultiSourceLogsQueryProcessor processor) {
    this.processor = processor;
  }

  @Override
  public QueryResponse queryAllSources(String stream, QueryRequest request, int limit)
      throws Exception {
    var queryCfg = new QueryConfig(true, true, true, true);
    return queryProcessorWithConfig(stream, request, queryCfg);
  }

  @Override
  public QueryResponse queryDiskAndBufferPool(String stream, QueryRequest request, int limit)
      throws Exception {
    var queryCfg = new QueryConfig(false, true, true, false);
    return queryProcessorWithConfig(stream, request, queryCfg);
  }

  public QueryResponse queryProcessorWithConfig(
      String stream, QueryRequest request, QueryConfig queryCfg) throws Exception {
    var filter = buildLogFilter(request.getFilter());
    var responses =
        processor.getLogs(stream, request.getStart(), request.getEnd(), filter, queryCfg);
    var views = responses.stream().map(BinaryLogRecordV1::toLogView).toList();
    return QueryResponse.builder().items(views).build();
  }

  public PageFilter<BinaryLogRecordV1, LogPageMetadata> buildLogFilter(FilterNode filterNode)
      throws BadRequestException {
    if (filterNode.getKind() == null) {
      throw new BadRequestException("Filter node kind cannot be null");
    }
    return switch (filterNode.getKind()) {
      case "AND" ->
          new AndPageFilter<>(
              buildLogFilter(filterNode.getLeft()), buildLogFilter(filterNode.getRight()));
      case "OR" ->
          new OrPageFilter<>(
              buildLogFilter(filterNode.getLeft()), buildLogFilter(filterNode.getRight()));
      case "LEVEL" -> new LevelPageFilter(filterNode.getLevelCode());
      case "TRACE" -> new LogPageTraceFilter(filterNode.getTraceId());
      case "BODY" -> new RegexPageFilter(filterNode.getRegex());
      default ->
          throw new BadRequestException("Not an accepted filter type: " + filterNode.getKind());
    };
  }
}
