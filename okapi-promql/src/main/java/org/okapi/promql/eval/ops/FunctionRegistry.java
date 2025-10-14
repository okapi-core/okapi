package org.okapi.promql.eval.ops;

// eval/ops/FunctionRegistry.java
import java.util.*;
import lombok.AllArgsConstructor;
import org.okapi.promql.eval.Evaluable;
import org.okapi.promql.eval.nodes.FunctionExpr;
import org.okapi.promql.eval.ts.StatisticsMerger;

@AllArgsConstructor
public final class FunctionRegistry {
  private final StatisticsMerger statisticsMerger;

  public Evaluable lower(FunctionExpr fn) {
    String name = fn.name.toLowerCase(Locale.ROOT);
    switch (name) {
      // Range time-stat functions
      case "rate" -> {
        return new RangeFuncEval(fn, RangeFuncEval.Kind.RATE);
      }
      case "irate" -> {
        return new RangeFuncEval(fn, RangeFuncEval.Kind.IRATE);
      }
      case "increase" -> {
        return new RangeFuncEval(fn, RangeFuncEval.Kind.INCREASE);
      }
      case "delta" -> {
        return new RangeFuncEval(fn, RangeFuncEval.Kind.DELTA);
      }
      case "idelta" -> {
        return new RangeFuncEval(fn, RangeFuncEval.Kind.IDELTA);
      }
      case "deriv" -> {
        return new RangeFuncEval(fn, RangeFuncEval.Kind.DERIV);
      }
      case "avg_over_time" -> {
        return new RangeStatEval(fn, RangeStatEval.Kind.AVG, statisticsMerger);
      }
      case "min_over_time" -> {
        return new RangeStatEval(fn, RangeStatEval.Kind.MIN, statisticsMerger);
      }
      case "max_over_time" -> {
        return new RangeStatEval(fn, RangeStatEval.Kind.MAX, statisticsMerger);
      }
      case "sum_over_time" -> {
        return new RangeStatEval(fn, RangeStatEval.Kind.SUM, statisticsMerger);
      }
      case "count_over_time" -> {
        return new RangeStatEval(fn, RangeStatEval.Kind.COUNT, statisticsMerger);
      }
      case "quantile_over_time" -> {
        return new RangeStatEval(fn, RangeStatEval.Kind.QUANTILE, statisticsMerger);
      }
      case "last_over_time" -> {
        return new RangeStatEval(fn, RangeStatEval.Kind.LAST, statisticsMerger);
      }
      case "present_over_time" -> {
        return new RangeStatEval(fn, RangeStatEval.Kind.PRESENT, statisticsMerger);
      }
      case "histogram_quantile" -> {
        return new HistogramQuantileEval(fn);
      }

      // Instant/vector utilities
      case "scalar" -> {
        return new ScalarFuncEval(fn);
      }
      case "timestamp" -> {
        return new TimestampFuncEval(fn);
      }
      case "time" -> {
        return new TimeFuncEval(fn);
      }
      case "sort" -> {
        return new SortEval(fn, /*desc*/ false);
      }
      case "sort_desc" -> {
        return new SortEval(fn, /*desc*/ true);
      }
      case "absent" -> {
        return new AbsentEval(fn);
      }

      // Simple unary map functions (already existed)
      case "abs", "ceil", "floor", "round" -> {
        return LazyUnaryFactory.lowerInstantUnary(fn);
      }

      default -> throw new UnsupportedOperationException("Function not implemented: " + fn.name);
    }
  }
}
