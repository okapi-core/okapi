package org.okapi.promql.eval;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.junit.jupiter.api.Test;
import org.okapi.promql.MockStatsMerger;
import org.okapi.promql.TestFixtures;
import org.okapi.promql.eval.VectorData.*;
import org.okapi.promql.eval.exceptions.EvaluationException;
import org.okapi.promql.parser.PromQLLexer;
import org.okapi.promql.parser.PromQLParser;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ScalarTimesAvgOverTimeTest {

  @Test
  void scalarTimesAvgOverTime_cpuUsage_twoInstances() throws EvaluationException {
    var cm = TestFixtures.buildCommonMocks();

    var exec = Executors.newFixedThreadPool(2);
    var merger = new MockStatsMerger();
    var evaluator = new ExpressionEvaluator(cm.client, cm.discovery, exec, merger);

    String promql = "2 * avg_over_time(cpu_usage[2m])";
    var lexer = new PromQLLexer(CharStreams.fromString(promql));
    var tokens = new CommonTokenStream(lexer);
    var parser = new PromQLParser(tokens);

    long start = cm.t1, end = cm.t3, step = cm.step;

    var res = evaluator.evaluate(promql, start, end, step, parser);
    assertEquals(ValueType.INSTANT_VECTOR, res.type());

    var iv = (InstantVectorResult) res;

    // For cpu_usage{instance="i1"}: values per minute 10,20,30,40
    // avg_over_time[2m] at t1,t2,t3 -> (10,20)->15 ; (20,30)->25 ; (30,40)->35 ; times 2 => 30, 50,
    // 70
    assertEquals(30f, find(iv, cm.cpuUsageApiI1, cm.t1), 1e-4);
    assertEquals(50f, find(iv, cm.cpuUsageApiI1, cm.t2), 1e-4);
    assertEquals(70f, find(iv, cm.cpuUsageApiI1, cm.t3), 1e-4);

    // For cpu_usage{instance="i2"}: 40,60,80,100
    // avgs: (40,60)->50 ; (60,80)->70 ; (80,100)->90 ; times 2 => 100, 140, 180
    assertEquals(100f, find(iv, cm.cpuUsageApiI2, cm.t1), 1e-4);
    assertEquals(140f, find(iv, cm.cpuUsageApiI2, cm.t2), 1e-4);
    assertEquals(180f, find(iv, cm.cpuUsageApiI2, cm.t3), 1e-4);
  }

  private static float find(InstantVectorResult iv, SeriesId series, long ts) {
    return iv.data().stream()
        .filter(s -> s.series().equals(series) && s.sample().ts() == ts)
        .findFirst()
        .orElseThrow()
        .sample()
        .value();
  }
}
