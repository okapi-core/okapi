package org.okapi.promql.eval;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.junit.jupiter.api.Test;
import org.okapi.promql.MockSeriesDiscovery;
import org.okapi.promql.MockStatistics;
import org.okapi.promql.MockStatsMerger;
import org.okapi.promql.TestFixtures;
import org.okapi.promql.eval.exceptions.EvaluationException;
import org.okapi.promql.parser.PromQLLexer;
import org.okapi.promql.parser.PromQLParser;
import org.okapi.promql.eval.VectorData.*;

import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class GroupLeftArithmeticTest {

  @Test
  void add_with_on_job_group_left_instance() throws EvaluationException {
    var cm = TestFixtures.buildCommonMocks();
    var exec = Executors.newFixedThreadPool(2);
    var merger = new MockStatsMerger();
    var evaluator = new ExpressionEvaluator(cm.client, cm.discovery, exec, merger);

    // Add request avg to replicas (1 RHS per job). RHS value=3; group_left expands RHS to both
    // instances.
    // Prepare additional data for requests per instance
    // i1: 100,200,300,400 ; i2: 10,20,30,40
    // Extend client directly:
    cm.client.put(
        "requests", cm.cpuUsageApiI1Tags, cm.t0, new MockStatistics(java.util.List.of(100f)));
    cm.client.put(
        "requests", cm.cpuUsageApiI1Tags, cm.t1, new MockStatistics(java.util.List.of(200f)));
    cm.client.put(
        "requests", cm.cpuUsageApiI1Tags, cm.t2, new MockStatistics(java.util.List.of(300f)));
    cm.client.put(
        "requests", cm.cpuUsageApiI1Tags, cm.t3, new MockStatistics(java.util.List.of(400f)));

    cm.client.put(
        "requests", cm.cpuUsageApiI2Tags, cm.t0, new MockStatistics(java.util.List.of(10f)));
    cm.client.put(
        "requests", cm.cpuUsageApiI2Tags, cm.t1, new MockStatistics(java.util.List.of(20f)));
    cm.client.put(
        "requests", cm.cpuUsageApiI2Tags, cm.t2, new MockStatistics(java.util.List.of(30f)));
    cm.client.put(
        "requests", cm.cpuUsageApiI2Tags, cm.t3, new MockStatistics(java.util.List.of(40f)));

    // Also tell discovery about requests series
    // NOTE: If your MockSeriesDiscovery is immutable, create it initially with these SeriesIds in
    // the fixture instead.
    // For brevity in this test we rebuild discovery here:
    var discovery =
        new MockSeriesDiscovery(
            java.util.List.of(
                cm.httpRequestsCounterApi,
                cm.cpuUsageApiI1,
                cm.cpuUsageApiI2,
                cm.memI1,
                cm.replicas,
                new SeriesId("requests", new Labels(cm.cpuUsageApiI1Tags)),
                new SeriesId("requests", new Labels(cm.cpuUsageApiI2Tags))));
    var evaluator2 = new ExpressionEvaluator(cm.client, discovery, exec, merger);

    String promql =
        "avg_over_time(requests[2m]) + on (job) group_left (instance) avg_over_time(pod_replicas[2m])";
    var parser =
        new PromQLParser(new CommonTokenStream(new PromQLLexer(CharStreams.fromString(promql))));

    long t = cm.t2; // evaluate at t2
    var res = evaluator2.evaluate(promql, t, t, cm.step, parser);
    assertEquals(ValueType.INSTANT_VECTOR, res.type());

    var iv = (InstantVectorResult) res;

    // Compute expectations:
    // i1: avg(requests[2m]) at t2 => (200,300)->250 ; + replicas(3) => 253
    // i2: (20,30)->25 ; + 3 => 28
    float i1 = sample(iv, new SeriesId("requests", new Labels(cm.cpuUsageApiI1Tags)), t);
    float i2 = sample(iv, new SeriesId("requests", new Labels(cm.cpuUsageApiI2Tags)), t);
    assertEquals(253f, i1, 1e-4);
    assertEquals(28f, i2, 1e-4);
  }

  private static float sample(InstantVectorResult iv, SeriesId series, long ts) {
    return iv.data().stream()
        .filter(s -> s.series().equals(series) && s.sample().ts() == ts)
        .findFirst()
        .orElseThrow()
        .sample()
        .value();
  }
}
