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

import static org.junit.jupiter.api.Assertions.assertThrows;

public class ManyToManyErrorTest {

  @Test
  void on_job_arithmetic_without_grouping_throws() {
    var cm = TestFixtures.buildCommonMocks();
    var exec = Executors.newFixedThreadPool(2);
    var merger = new MockStatsMerger();
    var evaluator = new ExpressionEvaluator(cm.client, cm.discovery, exec, merger);

    long t = cm.t2;

    // left: two instances for job="api"; right: also two instances (reuse cpu as both sides) =>
    // many-to-many
    String q = "avg_over_time(cpu_usage[2m]) + on (job) avg_over_time(cpu_usage[2m])";
    var parser =
        new PromQLParser(new CommonTokenStream(new PromQLLexer(CharStreams.fromString(q))));

    assertThrows(EvaluationException.class, () -> evaluator.evaluate(q, t, t, cm.step, parser));
  }
}
