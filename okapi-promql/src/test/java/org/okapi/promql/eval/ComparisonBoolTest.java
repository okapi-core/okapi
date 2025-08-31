package org.okapi.promql.eval;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.okapi.promql.extractor.TimeSeriesExtractor.findValue;

import java.util.concurrent.Executors;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.junit.jupiter.api.Test;
import org.okapi.promql.MockStatsMerger;
import org.okapi.promql.TestFixtures;
import org.okapi.promql.eval.exceptions.EvaluationException;
import org.okapi.promql.parser.PromQLLexer;
import org.okapi.promql.parser.PromQLParser;

public class ComparisonBoolTest {

  @Test
  void greaterThan_bool_over_avg_over_time() throws EvaluationException {
    var cm = TestFixtures.buildCommonMocks();
    var exec = Executors.newFixedThreadPool(2);
    var merger = new MockStatsMerger();
    var evaluator = new ExpressionEvaluator(cm.client, cm.discovery, exec, merger);

    // 2 * avg_over_time(cpu_usage[2m]) > bool 60
    String promql = "2 * avg_over_time(cpu_usage[2m]) > bool 60";
    var parser =
        new PromQLParser(new CommonTokenStream(new PromQLLexer(CharStreams.fromString(promql))));

    long start = cm.t1, end = cm.t3, step = cm.step;
    var res = evaluator.evaluate(promql, start, end, step, parser);
    assertEquals(ValueType.INSTANT_VECTOR, res.type());
    var iv = (InstantVectorResult) res;

    // From earlier: cpu i1 -> 2*avg = 30, 50, 70  => bool>60 -> 0,0,1
    assertEquals(0f, findValue(iv, cm.cpuUsageApiI1, cm.t1), 1e-4);
    assertEquals(0f, findValue(iv, cm.cpuUsageApiI1, cm.t2), 1e-4);
    assertEquals(1f, findValue(iv, cm.cpuUsageApiI1, cm.t3), 1e-4);

    // cpu i2 -> 2*avg = 100, 140, 180 => all true
    assertEquals(1f, findValue(iv, cm.cpuUsageApiI2, cm.t1), 1e-4);
    assertEquals(1f, findValue(iv, cm.cpuUsageApiI2, cm.t2), 1e-4);
    assertEquals(1f, findValue(iv, cm.cpuUsageApiI2, cm.t3), 1e-4);
  }
}
