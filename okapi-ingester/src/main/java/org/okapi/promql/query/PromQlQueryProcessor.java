/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.promql.query;

import java.util.*;
import java.util.concurrent.ExecutorService;
import lombok.RequiredArgsConstructor;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.okapi.exceptions.BadRequestException;
import org.okapi.promql.eval.ExpressionEvaluator;
import org.okapi.promql.eval.ExpressionResult;
import org.okapi.promql.eval.VectorData;
import org.okapi.promql.eval.exceptions.EvaluationException;
import org.okapi.promql.eval.ts.StatisticsMerger;
import org.okapi.promql.eval.visitor.DurationUtil;
import org.okapi.promql.parser.PromQLLexer;
import org.okapi.promql.parser.PromQLParser;
import org.okapi.promql.runtime.SeriesDiscoveryFactory;
import org.okapi.promql.runtime.TsClientFactory;
import org.okapi.promql.time.PromQlDateParser;
import org.okapi.rest.promql.GetPromQlResponse;
import org.okapi.rest.promql.PromQlData;
import org.okapi.rest.promql.PromQlResponseMapper;

@RequiredArgsConstructor
public class PromQlQueryProcessor {

  final ExecutorService exec;
  final StatisticsMerger merger;
  final TsClientFactory metricsClientFactory;
  final SeriesDiscoveryFactory seriesDiscoveryFactory;
  UrlUnEscaper urlUnEscaper = new UrlUnEscaper();

  public ExpressionResult queryRange(
      String tenantId, String promql, String start, String end, String step)
      throws EvaluationException, BadRequestException {
    var st = PromQlDateParser.parseAsUnix(start);
    if (st.isEmpty()) {
      throw new BadRequestException(String.format("Date: %s is not a valid start-date", start));
    }
    var en = PromQlDateParser.parseAsUnix(end);
    if (en.isEmpty()) {
      throw new BadRequestException(String.format("Date: %s is not a valid start-date", end));
    }
    Long stepMs;
    try {
      stepMs = DurationUtil.parseToMillis(step);
    } catch (IllegalArgumentException e) {
      throw new BadRequestException(String.format("Date: %s is not a valid step", step));
    }
    return queryRange(tenantId, promql, st.get(), en.get(), stepMs);
  }

  public ExpressionResult queryRange(
      String tenantId, String promql, long startMs, long endMs, long stepMs)
      throws EvaluationException, BadRequestException {
    var lexer = new PromQLLexer(CharStreams.fromString(promql));
    var tokens = new CommonTokenStream(lexer);
    var client = metricsClientFactory.getClient(tenantId);
    if (client.isEmpty()) {
      throw new BadRequestException("Cluster is unavailable as we may be under maintenance.");
    }
    var discovery = seriesDiscoveryFactory.get(tenantId);
    var parser = new PromQLParser(tokens);
    var evaluator = new ExpressionEvaluator(client.get(), discovery, exec, merger);
    return evaluator.evaluate(promql, startMs, endMs, stepMs, parser);
  }

  public ExpressionResult queryPointInTime(String tenantId, String promql, long instant)
      throws EvaluationException, BadRequestException {
    var lexer = new PromQLLexer(CharStreams.fromString(promql));
    var tokens = new CommonTokenStream(lexer);
    var client = metricsClientFactory.getClient(tenantId);
    if (client.isEmpty()) {
      throw new BadRequestException("Cluster is unavailable as we may be resharding.");
    }
    var discovery = seriesDiscoveryFactory.get(tenantId);
    var parser = new PromQLParser(tokens);
    var evaluator = new ExpressionEvaluator(client.get(), discovery, exec, merger);
    return evaluator.evaluateAt(promql, instant, parser);
  }

  public GetPromQlResponse<PromQlData<?>> queryRangeApi(
      String tenantId, String promQl, String start, String end, String step)
      throws BadRequestException, EvaluationException {
    var result = queryRange(tenantId, promQl, start, end, step);
    return PromQlResponseMapper.toResult(result, PromQlResponseMapper.RETURN_TYPE.MATRIX);
  }

  public GetPromQlResponse<PromQlData<?>> queryInstantApi(
      String tenantId, String promQl, String time) throws BadRequestException, EvaluationException {
    Long now;
    if (time == null) {
      now = System.currentTimeMillis();
    } else {
      var instant = PromQlDateParser.parseAsUnix(time);
      if (instant.isEmpty()) {
        throw new BadRequestException(String.format("Got illegal date %s.", time));
      }
      now = instant.get();
    }
    var result = queryPointInTime(tenantId, promQl, now);
    return PromQlResponseMapper.toResult(result, PromQlResponseMapper.RETURN_TYPE.VECTOR_OR_SCALAR);
  }

  public Set<VectorData.SeriesId> getMatches(
      String tenantId, List<String> conditions, long start, long end) throws BadRequestException {
    var allMatches = new HashSet<VectorData.SeriesId>();
    if (conditions == null || conditions.isEmpty()) {
      var discovery = seriesDiscoveryFactory.get(tenantId);
      allMatches.addAll(discovery.expand(null, Collections.emptyList(), start, end));
      return Collections.unmodifiableSet(allMatches);
    }
    for (var match : conditions) {
      var lexer = new PromQLLexer(CharStreams.fromString(match));
      var tokens = new CommonTokenStream(lexer);
      var discovery = seriesDiscoveryFactory.get(tenantId);
      var client = metricsClientFactory.getClient(tenantId);
      if (client.isEmpty()) {
        throw new BadRequestException("Cluster is unavailable as we may be resharding.");
      }
      var parser = new PromQLParser(tokens);
      var evaluator = new ExpressionEvaluator(client.get(), discovery, exec, merger);
      var matchingSeries = evaluator.find(parser, start, end);
      allMatches.addAll(matchingSeries);
    }
    return Collections.unmodifiableSet(allMatches);
  }

  public GetPromQlResponse<List<String>> queryLabelNamesApi(
      String tenantId, List<String> matches, String start, String end) throws BadRequestException {
    var conditions = matches != null ? matches : Collections.<String>emptyList();
    long st = 0L;
    long en = System.currentTimeMillis();
    if (start != null) {
      var parsed = PromQlDateParser.parseAsUnix(start);
      if (parsed.isEmpty()) {
        throw new BadRequestException(String.format("Got illegal date %s.", start));
      }
      st = parsed.get();
    }
    if (end != null) {
      var parsed = PromQlDateParser.parseAsUnix(end);
      if (parsed.isEmpty()) {
        throw new BadRequestException(String.format("Got illegal date %s.", end));
      }
      en = parsed.get();
    }
    var matchingSeriesIds = getMatches(tenantId, conditions, st, en);
    var labelNames = new HashSet<String>();
    for (var id : matchingSeriesIds) {
      labelNames.add(PromQlResponseMapper.NAME);
      if (id.labels() != null && id.labels().tags() != null) {
        labelNames.addAll(id.labels().tags().keySet());
      }
    }
    var list = new ArrayList<>(labelNames);
    Collections.sort(list);
    return PromQlResponseMapper.mapStringList(list);
  }

  public GetPromQlResponse<List<String>> queryLabelsApi(
      String tenantId, String rawLabel, List<String> matches, String start, String end)
      throws BadRequestException {
    long st = 0L;
    long en = System.currentTimeMillis();
    if (start != null) {
      var parsed = PromQlDateParser.parseAsUnix(start);
      if (parsed.isEmpty()) {
        throw new BadRequestException(String.format("Got illegal date %s.", start));
      }
      st = parsed.get();
    }
    if (end != null) {
      var parsed = PromQlDateParser.parseAsUnix(end);
      if (parsed.isEmpty()) {
        throw new BadRequestException(String.format("Got illegal date %s.", end));
      }
      en = parsed.get();
    }
    var label = urlUnEscaper.unescape(rawLabel);
    var conditions = matches != null ? matches : Collections.<String>emptyList();
    var matchingSeriesIds = getMatches(tenantId, conditions, st, en);
    var labelValues = new HashSet<String>();
    if ("__name__".equals(label)) {
      for (var id : matchingSeriesIds) {
        if (id.metric() != null) {
          labelValues.add(id.metric());
        }
      }
    } else {
      for (var id : matchingSeriesIds) {
        if (id.labels() != null
            && id.labels().tags() != null
            && id.labels().tags().containsKey(label)) {
          labelValues.add(id.labels().tags().get(label));
        }
      }
    }
    return PromQlResponseMapper.mapStringList(new ArrayList<>(labelValues));
  }
}
