package org.okapi.promql.eval.ops;

// eval/ops/AggregateEval.java
import java.util.*;
import java.util.stream.*;
import lombok.AllArgsConstructor;
import org.okapi.promql.eval.*;
import org.okapi.promql.eval.VectorData.*;
import org.okapi.promql.eval.exceptions.EvaluationException;
import org.okapi.promql.eval.nodes.AggregateExpr;

@AllArgsConstructor
public class AggregateEval implements Evaluable {
  private final AggregateExpr node;

  @Override
  public ExpressionResult eval(EvalContext ctx) throws EvaluationException {
    if (node.args.isEmpty()) throw new IllegalArgumentException("aggregation expects arguments");
    String op = node.op.toLowerCase(Locale.ROOT);

    // Some aggregators need scalar parameter (topk/bottomk/quantile)
    int vecIdx = 0;
    Float param = null;
    if (op.equals("topk") || op.equals("bottomk") || op.equals("quantile")) {
      if (node.args.size() < 2)
        throw new IllegalArgumentException(op + " requires scalar parameter and vector");
      var pRes = node.args.get(0).lower().eval(ctx);
      if (!(pRes instanceof ScalarResult s))
        throw new IllegalArgumentException(op + " parameter must be scalar");
      param = s.value;
      vecIdx = 1;
    }
    var in = node.args.get(vecIdx).lower().eval(ctx);
    if (!(in instanceof InstantVectorResult iv))
      throw new IllegalArgumentException("aggregation expects instant-vector input");

    Map<GroupKey, List<SeriesSample>> groups = new HashMap<>();
    for (SeriesSample s : iv.data()) {
      GroupKey gk = groupKey(node.isBy, node.groupLabels, s.series().labels().tags());
      groups.computeIfAbsent(gk, k -> new ArrayList<>()).add(s);
    }

    List<SeriesSample> out = new ArrayList<>(groups.size());
    for (var e : groups.entrySet()) {
      var list = e.getValue();
      float v;
      switch (op) {
        case "sum" -> v = (float) list.stream().mapToDouble(ss -> ss.sample().value()).sum();
        case "avg" ->
            v =
                (float)
                    list.stream()
                        .mapToDouble(ss -> ss.sample().value())
                        .average()
                        .orElse(Double.NaN);
        case "min" ->
            v =
                (float)
                    list.stream().mapToDouble(ss -> ss.sample().value()).min().orElse(Double.NaN);
        case "max" ->
            v =
                (float)
                    list.stream().mapToDouble(ss -> ss.sample().value()).max().orElse(Double.NaN);
        case "count" -> v = (float) list.size();
        case "stddev" -> v = stddev(list);
        case "stdvar" -> v = stdvar(list);
        case "group" -> v = 1f;
        case "topk" -> {
          int k = Math.max(0, Math.round(param));
          list.sort((a, b) -> Float.compare(b.sample().value(), a.sample().value()));
          var top = list.subList(0, Math.min(k, list.size()));
          out.addAll(cloneSamplesWithAggName(top, "topk"));
          continue;
        }
        case "bottomk" -> {
          int k = Math.max(0, Math.round(param));
          list.sort((a, b) -> Float.compare(a.sample().value(), b.sample().value()));
          var bot = list.subList(0, Math.min(k, list.size()));
          out.addAll(cloneSamplesWithAggName(bot, "bottomk"));
          continue;
        }
        case "quantile" -> {
          double q = param;
          v = quantile(list, q);
        }
        default -> throw new UnsupportedOperationException("agg not implemented: " + node.op);
      }
      var labels = new Labels(e.getKey().labels());
      var id = new SeriesId(node.op, labels);
      long ts = iv.data().isEmpty() ? ctx.endMs : iv.data().get(0).sample().ts();
      out.add(new SeriesSample(id, new Sample(ts, v)));
    }
    return new InstantVectorResult(out);
  }

  private float quantile(List<SeriesSample> list, double q) {
    if (list.isEmpty()) return Float.NaN;
    var arr = list.stream().map(s -> s.sample().value()).sorted().toList();
    int n = arr.size();
    if (n == 1) return arr.get(0);
    double idx = q * (n - 1);
    int i = (int) Math.floor(idx);
    int j = (int) Math.ceil(idx);
    if (i == j) return arr.get(i);
    double frac = idx - i;
    return (float) (arr.get(i) * (1 - frac) + arr.get(j) * frac);
  }

  private float stddev(List<SeriesSample> list) {
    if (list.isEmpty()) return Float.NaN;
    double mean = list.stream().mapToDouble(s -> s.sample().value()).average().orElse(Double.NaN);
    double var =
        list.stream()
            .mapToDouble(
                s -> {
                  double d = s.sample().value() - mean;
                  return d * d;
                })
            .average()
            .orElse(Double.NaN);
    return (float) Math.sqrt(var);
  }

  private float stdvar(List<SeriesSample> list) {
    if (list.isEmpty()) return Float.NaN;
    double mean = list.stream().mapToDouble(s -> s.sample().value()).average().orElse(Double.NaN);
    double var =
        list.stream()
            .mapToDouble(
                s -> {
                  double d = s.sample().value() - mean;
                  return d * d;
                })
            .average()
            .orElse(Double.NaN);
    return (float) var;
  }

  private List<SeriesSample> cloneSamplesWithAggName(List<SeriesSample> list, String name) {
    List<SeriesSample> out = new ArrayList<>(list.size());
    for (var s : list) {
      var id = new SeriesId(name, s.series().labels());
      out.add(new SeriesSample(id, s.sample()));
    }
    return out;
  }

  private record GroupKey(Map<String, String> labels) {}

  private static GroupKey groupKey(
      boolean isBy, List<String> groupLabels, Map<String, String> src) {
    Map<String, String> m = new HashMap<>();
    if (groupLabels == null || groupLabels.isEmpty()) {
      for (var e : src.entrySet())
        if (!"__name__".equals(e.getKey())) m.put(e.getKey(), e.getValue());
      return new GroupKey(m);
    }
    if (isBy) {
      for (String k : groupLabels) {
        if (src.containsKey(k)) m.put(k, src.get(k));
      }
    } else {
      for (var e : src.entrySet())
        if (!groupLabels.contains(e.getKey())) m.put(e.getKey(), e.getValue());
    }
    return new GroupKey(m);
  }
}
