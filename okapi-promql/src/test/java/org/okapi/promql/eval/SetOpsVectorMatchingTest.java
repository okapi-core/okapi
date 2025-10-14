package org.okapi.promql.eval;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Set;
import java.util.concurrent.Executors;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.junit.jupiter.api.Test;
import org.okapi.promql.MockStatsMerger;
import org.okapi.promql.TestFixtures;
import org.okapi.promql.eval.exceptions.EvaluationException;
import org.okapi.promql.parser.PromQLLexer;
import org.okapi.promql.parser.PromQLParser;

public class SetOpsVectorMatchingTest {

  @Test
  void and_unless_on_instance() throws EvaluationException {
    var cm = TestFixtures.buildCommonMocks();
    var exec = Executors.newFixedThreadPool(2);
    var merger = new MockStatsMerger();
    var evaluator = new ExpressionEvaluator(cm.client, cm.discovery, exec, merger);

    // Build two range vectors reduced to instant via avg_over_time
    // A: avg_over_time(cpu_usage[2m])
    // B: avg_over_time(mem_usage[2m])   (exists only for instance i1)

    var parserAnd =
        new PromQLParser(
            new CommonTokenStream(
                new PromQLLexer(
                    CharStreams.fromString(
                        "avg_over_time(cpu_usage[2m]) and on (instance) avg_over_time(mem_usage[2m])"))));
    var resAnd = evaluator.evaluate("", cm.t2, cm.t2, cm.step, parserAnd);
    assertEquals(ValueType.INSTANT_VECTOR, resAnd.type());
    var ivAnd = (InstantVectorResult) resAnd;

    // Only instance i1 should survive "and"
    var instancesAnd = labelInstances(ivAnd);
    assertEquals(Set.of("i1"), instancesAnd);

    var parserUnless =
        new PromQLParser(
            new CommonTokenStream(
                new PromQLLexer(
                    CharStreams.fromString(
                        "avg_over_time(cpu_usage[2m]) unless on (instance) avg_over_time(mem_usage[2m])"))));
    var resUnless = evaluator.evaluate("", cm.t2, cm.t2, cm.step, parserUnless);
    assertEquals(ValueType.INSTANT_VECTOR, resUnless.type());
    var ivUnless = (InstantVectorResult) resUnless;

    // Only instance i2 should remain in "unless"
    var instancesUnless = labelInstances(ivUnless);
    assertEquals(Set.of("i2"), instancesUnless);
  }

  private static Set<String> labelInstances(InstantVectorResult iv) {
    var set = new java.util.HashSet<String>();
    for (var s : iv.data()) {
      set.add(s.series().labels().tags().get("instance"));
    }
    return set;
  }
}
