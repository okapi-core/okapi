package org.okapi.metrics.query.promql;

import java.util.concurrent.ExecutorService;
import lombok.AllArgsConstructor;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.okapi.promql.eval.ExpressionEvaluator;
import org.okapi.promql.eval.ExpressionResult;
import org.okapi.promql.eval.exceptions.EvaluationException;
import org.okapi.promql.eval.ts.StatisticsMerger;
import org.okapi.promql.parser.PromQLLexer;
import org.okapi.promql.parser.PromQLParser;

@AllArgsConstructor
public class PromQlQueryProcessor {

  ExecutorService exec;
  StatisticsMerger merger;
  TimeSeriesClientFactory metricsClientFactory;
  SeriesDiscoveryFactory pathSetDiscoveryClientFactory;

  public ExpressionResult process(String tenantId, String promql, long start, long end, long step)
      throws EvaluationException {
    var lexer = new PromQLLexer(CharStreams.fromString(promql));
    var tokens = new CommonTokenStream(lexer);
    var client = metricsClientFactory.getClient(tenantId);
    var discovery = pathSetDiscoveryClientFactory.get(tenantId);
    var parser = new PromQLParser(tokens);
    var evaluator = new ExpressionEvaluator(client, discovery, exec, merger);
    var result = evaluator.evaluate(promql, start, end, step, parser);
    return result;
  }
}
