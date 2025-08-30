package org.okapi.promql.eval;

// eval/ExpressionEvaluator.java
import java.util.concurrent.*;
import org.okapi.promql.eval.exceptions.EvaluationException;
import org.okapi.promql.eval.ts.RESOLUTION;
import org.okapi.promql.eval.ts.SeriesDiscovery;
import org.okapi.promql.eval.ts.StatisticsMerger;
import org.okapi.promql.eval.ts.TimeseriesClient;
import org.okapi.promql.eval.visitor.ExpressionVisitor;
import org.okapi.promql.parser.PromQLParser;

public final class ExpressionEvaluator {
  private final TimeseriesClient client;
  private final SeriesDiscovery discovery;
  private final ExecutorService exec;
  private final StatisticsMerger statisticsMerger;

  public ExpressionEvaluator(
      TimeseriesClient client,
      SeriesDiscovery discovery,
      ExecutorService exec,
      StatisticsMerger statisticsMerger) {
    this.client = client;
    this.discovery = discovery;
    this.exec = exec;
    this.statisticsMerger = statisticsMerger;
  }

  private static RESOLUTION chooseResolution(long stepMs) {
    if (stepMs <= 1_000L) return RESOLUTION.SECONDLY;
    if (stepMs <= 60_000L) return RESOLUTION.MINUTELY;
    return RESOLUTION.HOURLY;
  }

  public ExpressionResult evaluate(
      String promql, long startMs, long endMs, long stepMs, PromQLParser parser) throws EvaluationException {
    var tree = parser.expression(); // assume parser already constructed with token stream
    var logical = new ExpressionVisitor(statisticsMerger).visit(tree); // returns LogicalExpr
    var ctx =
        new EvalContext(startMs, endMs, stepMs, chooseResolution(stepMs), client, discovery, exec);
    return logical.lower().eval(ctx);
  }
}
