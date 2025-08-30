package org.okapi.promql.eval;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.junit.jupiter.api.Test;
import org.okapi.promql.MockStatsMerger;
import org.okapi.promql.TestFixtures;
import org.okapi.promql.eval.exceptions.EvaluationException;
import org.okapi.promql.parser.PromQLLexer;
import org.okapi.promql.parser.PromQLParser;

import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MissingMatchesTest {
  @Test
  void arithmeticWithNoMatchingLabels_producesEmptyVector() throws EvaluationException {
    var cm = TestFixtures.buildCommonMocks();
    var exec = Executors.newFixedThreadPool(2);
    var merger = new MockStatsMerger();
    var evaluator = new ExpressionEvaluator(cm.client, cm.discovery, exec, merger);

    long t = cm.t2;

    // cpu_usage has instance label; mem_usage exists only for i1.
    // Use on(service) which no series has — forces no matches.
    String q = "avg_over_time(cpu_usage[2m]) + on (service) avg_over_time(mem_usage[2m])";
    var parser =
        new PromQLParser(new CommonTokenStream(new PromQLLexer(CharStreams.fromString(q))));
    var res = evaluator.evaluate(q, t, t, cm.step, parser);

    assertEquals(ValueType.INSTANT_VECTOR, res.type());
    var iv = (InstantVectorResult) res;
    assertTrue(iv.data().isEmpty(), "No matching labels -> empty output");
  }
}
