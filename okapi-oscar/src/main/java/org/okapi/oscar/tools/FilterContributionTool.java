package org.okapi.oscar.tools;

import lombok.extern.slf4j.Slf4j;
import org.okapi.ingester.client.IngesterClient;
import org.okapi.oscar.spring.ConfigKeys;
import org.okapi.oscar.tools.responses.FilterContribution;
import org.okapi.parallel.ParallelExecutor;
import org.okapi.rest.traces.SpanQueryV2Request;
import org.okapi.rest.traces.SpanQueryV2SummaryResponse;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

@Slf4j
@Component
public class FilterContributionTool {

  IngesterClient client;
  ParallelExecutor executorService;

  public FilterContributionTool(
      IngesterClient client,
      @Value(ConfigKeys.CONTRIB_TOOL_THREAD_COUNT) int threadCount,
      @Value(ConfigKeys.CONTRIB_TOOL_THROTTLE) int throttle,
      @Value(ConfigKeys.CONTRIB_TOOL_DURATION_MILLIS) int durationMillis) {

    this.client = client;
    this.executorService =
        ParallelExecutor.of(threadCount, throttle, Duration.ofMillis(durationMillis));
  }

  @Tool(
      description =
"""
Purpose
Return per-filter match counts for a span query to explain which filters are narrowing results.
When to use
1) A span query returns zero results and you need to identify which filter is too restrictive.
2) You want to compare filter selectivity to refine a span search.
Examples
1) Filter A (serviceFilter) returns 0 while filter B (traceId) returns 12.
2) Filter A (httpFilters) returns 3 while filter B (timestampFilter) returns 50.
Hint
Use this tool when a span query returns zero matches; retry without any filter that yields a count of 0.
""")
  public FilterContribution getFilterContributions(@ToolParam SpanQueryV2Request request) {
    log.info("Calling with: {}", request);
    var contribBuilder = FilterContribution.builder();
    var builderMethods =
        new java.util.ArrayList<Function<Long, FilterContribution.FilterContributionBuilder>>();
    var queries = new java.util.ArrayList<Supplier<SpanQueryV2SummaryResponse>>();

    if (request.getTraceId() != null) {
      builderMethods.add(contribBuilder::traceIdFilterResultCount);
      queries.add(
          () ->
              client.getResultsSummary(
                  SpanQueryV2Request.builder().traceId(request.getTraceId()).build()));
    }
    if (request.getSpanId() != null) {
      builderMethods.add(contribBuilder::spanIdFilterResultCount);
      queries.add(
          () ->
              client.getResultsSummary(
                  SpanQueryV2Request.builder().spanId(request.getSpanId()).build()));
    }
    if (request.getKind() != null) {
      builderMethods.add(contribBuilder::kindFilterCount);
      queries.add(
          () ->
              client.getResultsSummary(
                  SpanQueryV2Request.builder().kind(request.getKind()).build()));
    }
    if (request.getDbFilters() != null) {
      builderMethods.add(contribBuilder::dbFiltersCount);
      queries.add(
          () ->
              client.getResultsSummary(
                  SpanQueryV2Request.builder().dbFilters(request.getDbFilters()).build()));
    }
    if (request.getDurationFilter() != null) {
      builderMethods.add(contribBuilder::durationFilterCount);
      queries.add(
          () ->
              client.getResultsSummary(
                  SpanQueryV2Request.builder()
                      .durationFilter(request.getDurationFilter())
                      .build()));
    }
    if (request.getHttpFilters() != null) {
      builderMethods.add(contribBuilder::httpFiltersCount);
      queries.add(
          () ->
              client.getResultsSummary(
                  SpanQueryV2Request.builder().httpFilters(request.getHttpFilters()).build()));
    }
    if (request.getServiceFilter() != null) {
      builderMethods.add(contribBuilder::serviceFilterCount);
      queries.add(
          () ->
              client.getResultsSummary(
                  SpanQueryV2Request.builder().serviceFilter(request.getServiceFilter()).build()));
    }
    if (request.getTimestampFilter() != null) {
      builderMethods.add(contribBuilder::timestampFilterCount);
      queries.add(
          () ->
              client.getResultsSummary(
                  SpanQueryV2Request.builder()
                      .timestampFilter(request.getTimestampFilter())
                      .build()));
    }

    var stringAttributeCounts = new java.util.LinkedHashMap<String, Long>();
    if (request.getStringAttributesFilter() != null) {
      for (var filter : request.getStringAttributesFilter()) {
        if (filter == null || filter.getKey() == null) {
          continue;
        }
        var summary =
            client.getResultsSummary(
                SpanQueryV2Request.builder().stringAttributesFilter(List.of(filter)).build());
        stringAttributeCounts.put(filter.getKey(), summary.getCount());
      }
      if (!stringAttributeCounts.isEmpty()) {
        contribBuilder.stringAttributesFilterCount(stringAttributeCounts);
      }
    }

    var numberAttributeCounts = new java.util.LinkedHashMap<String, Long>();
    if (request.getNumberAttributesFilter() != null) {
      for (var filter : request.getNumberAttributesFilter()) {
        if (filter == null || filter.getKey() == null) {
          continue;
        }
        var summary =
            client.getResultsSummary(
                SpanQueryV2Request.builder().numberAttributesFilter(List.of(filter)).build());
        numberAttributeCounts.put(filter.getKey(), summary.getCount());
      }
      if (!numberAttributeCounts.isEmpty()) {
        contribBuilder.numberAttributesFilterCount(numberAttributeCounts);
      }
    }

    var results =
        executorService.submit(queries).stream().map(SpanQueryV2SummaryResponse::getCount).toList();
    for (int i = 0; i < results.size(); i++) {
      builderMethods.get(i).apply(results.get(i));
    }
    var contributions = contribBuilder.build();
    log.info("Got contributions: {}", contributions);
    return contributions;
  }
}
