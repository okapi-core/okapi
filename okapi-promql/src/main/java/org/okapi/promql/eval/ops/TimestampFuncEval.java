package org.okapi.promql.eval.ops;

import lombok.AllArgsConstructor;
import org.okapi.promql.eval.*;
import org.okapi.promql.eval.VectorData.*;
import org.okapi.promql.eval.exceptions.EvaluationException;
import org.okapi.promql.eval.nodes.FunctionExpr;

@AllArgsConstructor
public class TimestampFuncEval implements Evaluable {
  private final FunctionExpr fn;

  @Override
  public ExpressionResult eval(EvalContext ctx) throws EvaluationException {
    if (fn.args.size() != 1)
      throw new IllegalArgumentException("timestamp(vector) expects one arg");
    var r = fn.args.get(0).lower().eval(ctx);
    if (r instanceof InstantVectorResult iv) {
      // produce same labels but value=timestamp in seconds
      var out = new java.util.ArrayList<SeriesSample>(iv.data().size());
      for (var s : iv.data()) {
        float secs = s.sample().ts() / 1000f;
        out.add(new SeriesSample(s.series(), new Sample(s.sample().ts(), secs)));
      }
      return new InstantVectorResult(out);
    }
    if (r instanceof ScalarResult s) {
      return new ScalarResult(ctx.endMs / 1000f);
    }
    throw new IllegalArgumentException("timestamp expects instant vector or scalar");
  }
}
