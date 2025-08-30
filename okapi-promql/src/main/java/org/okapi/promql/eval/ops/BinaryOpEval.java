// eval/ops/BinaryOpEval.java
package org.okapi.promql.eval.ops;

import org.okapi.promql.eval.*;
import org.okapi.promql.eval.exceptions.EvaluationException;
import org.okapi.promql.eval.nodes.BinaryOpExpr;
import org.okapi.promql.eval.nodes.MatchSpec;
import org.okapi.promql.eval.VectorData.*;

import java.util.*;
import java.util.function.Function;

/**
 * Implements arithmetic, comparisons (with/without 'bool'), and set ops (and/or/unless) with full
 * vector matching: - ON / IGNORING to build join keys - group_left / group_right with optional
 * include labels from the "many" side
 *
 * <p>Semantics summary (PromQL-compatible): - Arithmetic ( + - * / % ^ ): vector-vector: 1:1 by
 * key; with group_left/right allow many-to-one; result labels come from base side scalar-vector /
 * vector-scalar already handled elsewhere - Comparisons (== != > < >= <=): without 'bool': filter
 * semantics — keep LHS samples that satisfy comparison; values are LHS values with 'bool' : boolean
 * vector — keep LHS/RHS matches and set value to 1 for true, 0 for false - Set ops: and : keep LHS
 * samples with a match on RHS (value from LHS) or : union; if both sides provide sample for same
 * output labels, prefer LHS unless : keep LHS samples with NO match on RHS
 */
public final class BinaryOpEval implements Evaluable {
  private final BinaryOpExpr node;

  public BinaryOpEval(BinaryOpExpr node) {
    this.node = node;
  }

  @Override
  public ExpressionResult eval(EvalContext ctx) throws EvaluationException {
    var l = node.left.lower().eval(ctx);
    var r = node.right.lower().eval(ctx);

    // Scalar-Scalar arithmetic or comparisons
    if (l instanceof ScalarResult ls && r instanceof ScalarResult rs) {
      return evalScalarScalar(ls, rs);
    }

    // Scalar-Vector & Vector-Scalar paths (arithmetic + comparisons)
    if (l instanceof ScalarResult ls2 && r instanceof InstantVectorResult rv2) {
      return evalScalarVector(ls2.value, rv2);
    }
    if (l instanceof InstantVectorResult lv2 && r instanceof ScalarResult rs2) {
      return evalVectorScalar(lv2, rs2.value);
    }

    // Vector-Vector
    if (l instanceof InstantVectorResult lv && r instanceof InstantVectorResult rv) {
      return evalVectorVector(lv, rv, ctx);
    }

    throw new UnsupportedOperationException("Unsupported operand types for binop " + node.op);
  }

  // ---------- Scalar-Scalar ----------
  private ExpressionResult evalScalarScalar(ScalarResult ls, ScalarResult rs) {
    String op = node.op;
    if (isArithmetic(op)) {
      return new ScalarResult(applyArith(ls.value, rs.value, op));
    } else if (isComparison(op)) {
      boolean ok = compare(ls.value, rs.value, op);
      return node.boolModifier
          ? new ScalarResult(ok ? 1f : 0f)
          : (ok ? ls : new ScalarResult(Float.NaN));
    } else if (isSetOp(op)) {
      // set ops on scalars reduce to simple presence logic; keep LHS for "and", union for "or",
      // drop for "unless"
      return switch (op) {
        case "and" -> ls;
        case "or" -> ls; // preferring LHS
        case "unless" -> new ScalarResult(Float.NaN);
        default -> throw new IllegalStateException();
      };
    }
    throw new UnsupportedOperationException("Unknown op " + op);
  }

  // ---------- Scalar-Vector ----------
  private ExpressionResult evalScalarVector(float s, InstantVectorResult v) {
    String op = node.op;
    if (isArithmetic(op)) {
      return mapVector(v, val -> applyArith(s, val, op));
    } else if (isComparison(op)) {
      if (node.boolModifier) {
        return mapVector(v, val -> compare(s, val, op) ? 1f : 0f);
      } else {
        // filter: keep sample if comparison true, drop otherwise
        return filterVector(v, val -> compare(s, val, op));
      }
    } else if (isSetOp(op)) {
      // set ops with scalar reduce to presence: treat scalar side as "present"
      return switch (op) {
        case "and" -> v; // scalar present → keep all vector samples
        case "or" -> v; // union → vector dominates
        case "unless" ->
            new InstantVectorResult(
                List.of()); // scalar present removes everything from LHS if scalar on left; here
        // scalar is left
        default -> throw new IllegalStateException();
      };
    }
    throw new UnsupportedOperationException("Unknown op " + op);
  }

  // ---------- Vector-Scalar ----------
  private ExpressionResult evalVectorScalar(InstantVectorResult v, float s) {
    String op = node.op;
    if (isArithmetic(op)) {
      return mapVector(v, val -> applyArith(val, s, op));
    } else if (isComparison(op)) {
      if (node.boolModifier) {
        return mapVector(v, val -> compare(val, s, op) ? 1f : 0f);
      } else {
        return filterVector(v, val -> compare(val, s, op));
      }
    } else if (isSetOp(op)) {
      // set ops with scalar on RHS: presence logic
      return switch (op) {
        case "and" -> v;
        case "or" -> v;
        case "unless" ->
            v; // LHS unless scalar present — scalar present always → drop none? Prom doesn't define
        // this; keep LHS.
        default -> throw new IllegalStateException();
      };
    }
    throw new UnsupportedOperationException("Unknown op " + op);
  }

  private ExpressionResult evalVectorVector(
      InstantVectorResult left, InstantVectorResult right, EvalContext ctx)
      throws EvaluationException {
    String op = node.op.toLowerCase(java.util.Locale.ROOT);
    var ms = node.matchSpec;

    // Build join indices based on ON/IGNORING
    var leftIdx = indexByJoinKey(left.data(), ms);
    var rightIdx = indexByJoinKey(right.data(), ms);

    List<SeriesSample> out = new ArrayList<>();

    switch (op) {
      // ----- Set ops -----
      case "and" -> {
        for (var key : leftIdx.keySet()) {
          var lList = leftIdx.get(key);
          var rList = rightIdx.get(key);
          if (rList == null || rList.isEmpty()) continue;
          // 'and' keeps LHS samples/labels/values (for all matching LHS)
          out.addAll(lList);
        }
        return new InstantVectorResult(out);
      }
      case "or" -> {
        // Union; prefer LHS values when label sets collide
        Set<SeriesId> seen = new HashSet<>();
        for (var s : left.data()) {
          out.add(s);
          seen.add(s.series());
        }
        for (var s : right.data()) {
          if (!seen.contains(s.series())) out.add(s);
        }
        return new InstantVectorResult(out);
      }
      case "unless" -> {
        for (var key : leftIdx.keySet()) {
          var lList = leftIdx.get(key);
          var rList = rightIdx.get(key);
          if (rList != null && !rList.isEmpty()) continue; // drop matches
          out.addAll(lList);
        }
        return new InstantVectorResult(out);
      }

      // ----- Comparisons (filter or bool) and Arithmetic -----
      default -> {
        boolean isCmp = isComparison(op);
        boolean isAr = isArithmetic(op);
        if (!isCmp && !isAr) throw new UnsupportedOperationException("Unsupported op: " + op);

        final boolean allowGroupLeft = ms != null && ms.groupLeft;
        final boolean allowGroupRight = ms != null && ms.groupRight;
        if (allowGroupLeft && allowGroupRight) {
          throw new EvaluationException("cannot specify both group_left and group_right");
        }

        for (var key : leftIdx.keySet()) {
          var lList = leftIdx.get(key);
          var rList = rightIdx.get(key);

          if (rList == null || rList.isEmpty()) {
            // No match → drop for arithmetic/comparisons
            continue;
          }

          // Cardinality rules:
          if (!allowGroupLeft && !allowGroupRight) {
            // Strict 1:1 only
            if (lList.size() != 1 || rList.size() != 1) {
              throw new EvaluationException(
                  "many-to-many matching is not allowed without group_left/group_right");
            }
          }
          if (allowGroupLeft) {
            // N:1 (expand RHS to LHS)
            if (rList.size() != 1) {
              throw new EvaluationException("group_left requires exactly one RHS match per key");
            }
          }
          if (allowGroupRight) {
            // 1:N (expand LHS to RHS)
            if (lList.size() != 1) {
              throw new EvaluationException("group_right requires exactly one LHS match per key");
            }
          }

          if (allowGroupLeft) {
            var r = rList.get(0);
            for (var l : lList) {
              var v = combine(l, r, op, isCmp);
              if (v != null) out.add(v);
            }
          } else if (allowGroupRight) {
            var l = lList.get(0);
            for (var r : rList) {
              var v = combine(l, r, op, isCmp);
              if (v != null) out.add(v);
            }
          } else {
            // 1:1
            var l = lList.get(0);
            var r = rList.get(0);
            var v = combine(l, r, op, isCmp);
            if (v != null) out.add(v);
          }
        }

        return new InstantVectorResult(out);
      }
    }
  }

  private boolean isArithmetic(String op) {
    return switch (op) {
      case "+", "-", "*", "/", "%", "^" -> true;
      default -> false;
    };
  }

  private boolean isComparison(String op) {
    return switch (op) {
      case "==", "!=", ">", "<", ">=", "<=" -> true;
      default -> false;
    };
  }

  private boolean isSetOp(String op) {
    return switch (op) {
      case "and", "or", "unless" -> true;
      default -> false;
    };
  }

  private float applyArith(float a, float b, String op) {
    return switch (op) {
      case "+" -> a + b;
      case "-" -> a - b;
      case "*" -> a * b;
      case "/" -> (b == 0f ? Float.NaN : a / b);
      case "%" -> (b == 0f ? Float.NaN : a % b);
      case "^" -> (float) Math.pow(a, b);
      default -> throw new IllegalStateException("op " + op);
    };
  }

  private boolean compare(float a, float b, String op) {
    return switch (op) {
      case "==" -> Float.compare(a, b) == 0;
      case "!=" -> Float.compare(a, b) != 0;
      case ">" -> a > b;
      case "<" -> a < b;
      case ">=" -> a >= b;
      case "<=" -> a <= b;
      default -> throw new IllegalStateException("op " + op);
    };
  }

  private InstantVectorResult mapVector(InstantVectorResult iv, Function<Float, Float> op) {
    List<SeriesSample> out = new ArrayList<>(iv.data().size());
    for (var s : iv.data())
      out.add(
          new SeriesSample(s.series(), new Sample(s.sample().ts(), op.apply(s.sample().value()))));
    return new InstantVectorResult(out);
  }

  private InstantVectorResult filterVector(
      InstantVectorResult iv, java.util.function.Predicate<Float> pred) {
    List<SeriesSample> out = new ArrayList<>();
    for (var s : iv.data()) {
      if (pred.test(s.sample().value())) out.add(s);
    }
    return new InstantVectorResult(out);
  }

  // Build join key → samples map for a side
  private Map<JoinKey, List<SeriesSample>> indexByJoinKey(
      List<SeriesSample> samples, MatchSpec ms) {
    Map<JoinKey, List<SeriesSample>> map = new HashMap<>();
    for (var s : samples) {
      var key = buildJoinKey(s.series().labels().tags(), ms);
      if (key == null) continue; // <<< skip non-matching series for ON(...)
      map.computeIfAbsent(key, k -> new ArrayList<>()).add(s);
    }
    return map;
  }

  // Key according to ON/IGNORING; default: IGNORING {__name__}
  private JoinKey buildJoinKey(Map<String, String> labels, MatchSpec ms) {
    Map<String, String> key = new TreeMap<>();
    if (ms == null) {
      // Default is IGNORING __name__
      for (var e : labels.entrySet())
        if (!e.getKey().equals("__name__")) key.put(e.getKey(), e.getValue());
      return new JoinKey(key);
    }
    if (ms.mode == MatchSpec.Mode.ON) {
      // NEW: if any ON label is missing — no match for this series
      for (String k : ms.labels) {
        String v = labels.get(k);
        if (v == null) return null; // <<< added line
        key.put(k, v);
      }
      return new JoinKey(key);
    } else { // IGNORING
      for (var e : labels.entrySet()) {
        if (e.getKey().equals("__name__")) continue;
        if (!ms.labels.contains(e.getKey())) key.put(e.getKey(), e.getValue());
      }
      return new JoinKey(key);
    }
  }

  private SeriesSample combine(SeriesSample l, SeriesSample r, String op, boolean isCmp) {
    float a = l.sample().value();
    float b = r.sample().value();
    long ts = l.sample().ts(); // Prom uses LHS timestamps/labels as base
    if (isCmp) {
      boolean ok = compare(a, b, op);
      if (node.boolModifier) {
        return new SeriesSample(l.series(), new Sample(ts, ok ? 1f : 0f));
      } else {
        return ok ? l : null;
      }
    } else {
      float v = applyArith(a, b, op);
      // Build output labels from LHS; if group_left/right includes are specified, copy those extra
      // RHS labels
      SeriesId outId = maybeIncludeLabels(l.series(), r.series(), node.matchSpec);
      return new SeriesSample(outId, new Sample(ts, v));
    }
  }

  private SeriesId maybeIncludeLabels(SeriesId left, SeriesId right, MatchSpec ms) {
    if (ms == null || (ms.include == null || ms.include.isEmpty())) return left;
    Map<String, String> out = new HashMap<>(left.labels().tags());
    for (String k : ms.include) {
      String v = right.labels().tags().get(k);
      if (v != null) out.put(k, v);
    }
    return new SeriesId(left.metric(), new Labels(out));
  }

  private static final class JoinKey {
    final Map<String, String> labels;

    JoinKey(Map<String, String> labels) {
      this.labels = labels;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof JoinKey k)) return false;
      return Objects.equals(labels, k.labels);
    }

    @Override
    public int hashCode() {
      return Objects.hash(labels);
    }

    @Override
    public String toString() {
      return labels.toString();
    }
  }
}
