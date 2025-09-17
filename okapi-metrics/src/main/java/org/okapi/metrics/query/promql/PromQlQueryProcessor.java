package org.okapi.metrics.query.promql;

import java.util.*;
import java.util.concurrent.ExecutorService;
import lombok.AllArgsConstructor;
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
import org.okapi.rest.promql.GetPromQlResponse;
import org.okapi.rest.promql.PromQlData;

@AllArgsConstructor
public class PromQlQueryProcessor {

  ExecutorService exec;
  StatisticsMerger merger;
  TsClientFactory metricsClientFactory;
  SeriesDiscoveryFactory seriesDiscoveryFactory;

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
      throw new BadRequestException("Cluster is unavailable as we may be resharding.");
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
    var result = evaluator.evaluateAt(promql, instant, parser);
    return result;
  }

  public GetPromQlResponse<PromQlData<?>> queryRangeApi(
      String tenantId, String promQl, String start, String end, String step)
      throws BadRequestException, EvaluationException {
    var result = queryRange(tenantId, promQl, start, end, step);
    return PromToResponseMapper.toResult(result, PromToResponseMapper.RETURN_TYPE.MATRIX);
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
    return PromToResponseMapper.toResult(result, PromToResponseMapper.RETURN_TYPE.VECTOR_OR_SCALAR);
  }

  public Set<VectorData.SeriesId> getMatches(
      String tenantId, List<String> conditions, long start, long end) throws BadRequestException {
    var allMatches = new HashSet<VectorData.SeriesId>();
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
      for (var series : matchingSeries) {
        allMatches.add(series);
      }
    }

    return Collections.unmodifiableSet(allMatches);
  }

  public GetPromQlResponse<List<String>> queryMatchApi(
      String tenantId, List<String> matches, String start, String end) throws BadRequestException {
    var conditions = matches != null ? matches : Collections.<String>emptyList();
    var st = DurationUtil.parseToMillis(start);
    var en = DurationUtil.parseToMillis(end);
    var matchingSeriesIds = getMatches(tenantId, conditions, st, en);
    return PromToResponseMapper.mapStringList(
        matchingSeriesIds.stream().map(VectorData.SeriesId::metric).toList());
  }

  public GetPromQlResponse<List<String>> queryLabelsApi(
      String tenantId, String label, List<String> matches, String start, String end)
      throws BadRequestException {
    var st = DurationUtil.parseToMillis(start);
    var en = DurationUtil.parseToMillis(end);
    var conditions = matches != null ? matches : Collections.<String>emptyList();
    var matchingSeriesIds = getMatches(tenantId, conditions, st, en);
    var labelValues = new HashSet<String>();
    for (var id : matchingSeriesIds) {
      if (id.labels() != null
          && id.labels().tags() != null
          && id.labels().tags().containsKey(label)) {
        labelValues.add(id.labels().tags().get(label));
      }
    }
    return PromToResponseMapper.mapStringList(new ArrayList<>(labelValues));
  }
}
