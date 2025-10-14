package org.okapi.promql.eval.ops;

// eval/ops/LazyUnaryFactory.java
import java.util.*;
import java.util.function.*;
import org.okapi.promql.eval.Evaluable;
import org.okapi.promql.eval.InstantVectorResult;
import org.okapi.promql.eval.VectorData.*;
import org.okapi.promql.eval.nodes.FunctionExpr;

public class LazyUnaryFactory {
  public LazyUnaryFactory() {}

  static Evaluable lowerInstantUnary(FunctionExpr fn) {
    if (fn.args.size() != 1) throw new IllegalArgumentException(fn.name + " expects one arg");
    Function<Float, Float> op =
        switch (fn.name.toLowerCase(Locale.ROOT)) {
          case "abs" -> Math::abs;
          case "ceil" -> x -> (float) Math.ceil(x);
          case "floor" -> x -> (float) Math.floor(x);
          case "round" -> x -> (float) Math.rint(x);
          case "clamp_min" -> {
            float k = 0f;
            yield v -> Math.max(v, k);
          } // extend to read second arg if needed
          case "clamp_max" -> {
            float k = 0f;
            yield v -> Math.min(v, k);
          }
          default -> throw new UnsupportedOperationException("unary fn: " + fn.name);
        };
    var inner = fn.args.get(0).lower();
    return ctx -> {
      var res = inner.eval(ctx);
      if (res instanceof InstantVectorResult iv) {
        List<SeriesSample> out = new ArrayList<>(iv.data().size());
        for (var s : iv.data())
          out.add(
              new SeriesSample(
                  s.series(), new Sample(s.sample().ts(), op.apply(s.sample().value()))));
        return new InstantVectorResult(out);
      }
      throw new IllegalArgumentException("unary fn expects instant-vector");
    };
  }
}
