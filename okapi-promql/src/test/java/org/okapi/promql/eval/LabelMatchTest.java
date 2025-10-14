package org.okapi.promql.eval;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.junit.jupiter.api.Test;
import org.okapi.promql.MockStatsMerger;
import org.okapi.promql.TestFixtures;
import org.okapi.promql.parser.PromQLLexer;
import org.okapi.promql.parser.PromQLParser;

public class LabelMatchTest {

  @Test
  public void testLabelMatch() {
    var commonMocks = TestFixtures.buildCommonMocks();
    var discovery = commonMocks.discovery;
    var client = commonMocks.client;
    var exec = Executors.newFixedThreadPool(2);
    var merger = new MockStatsMerger(); // provided no-arg implementation
    var evaluator = new ExpressionEvaluator(client, discovery, exec, merger);

    String promql = "http_requests_counter";
    var lexer = new PromQLLexer(CharStreams.fromString(promql));
    var tokens = new CommonTokenStream(lexer);
    var parser = new PromQLParser(tokens);
    var list = evaluator.find(parser, commonMocks.t0, commonMocks.t3);
    assertFalse(list.isEmpty());
  }

  private static List<VectorData.SeriesId> find(
      ExpressionEvaluator evaluator, String selector, long start, long end) {
    var lexer = new PromQLLexer(CharStreams.fromString(selector));
    var tokens = new CommonTokenStream(lexer);
    var parser = new PromQLParser(tokens);
    return evaluator.find(parser, start, end);
  }

  @Test
  void regex_metricName_positive_matchesCpuOnly() {
    var cm = TestFixtures.buildCommonMocks();
    var evaluator =
        new ExpressionEvaluator(
            cm.client, cm.discovery, Executors.newFixedThreadPool(2), new MockStatsMerger());

    // Match all metrics whose __name__ starts with "cpu_"
    var out = find(evaluator, "{__name__=~\"cpu_.*\"}", cm.t0, cm.t3);

    assertFalse(out.isEmpty(), "expected matches for cpu_.*");
    // Expect only cpu_usage (two instances). Be tolerant on size but enforce all metrics start with
    // cpu_
    assertTrue(
        out.stream().allMatch(s -> s.metric().startsWith("cpu_")),
        "non-cpu metric leaked into result");
    // In our fixture, cpu_usage has two instances (i1, i2)
    var instances =
        out.stream()
            .map(s -> s.labels().tags().get("instance"))
            .collect(java.util.stream.Collectors.toSet());
    assertEquals(Set.of("i1", "i2"), instances, "expected cpu_usage for i1 and i2");
  }

  @Test
  void regex_metricName_negative_excludesCpu() {
    var cm = TestFixtures.buildCommonMocks();
    var evaluator =
        new ExpressionEvaluator(
            cm.client, cm.discovery, Executors.newFixedThreadPool(2), new MockStatsMerger());

    // Exclude any metric whose name starts with cpu_
    var out = find(evaluator, "{__name__!~\"cpu_.*\"}", cm.t0, cm.t3);

    assertFalse(out.isEmpty(), "expected some non-cpu metrics in fixture");
    assertTrue(
        out.stream().noneMatch(s -> s.metric().startsWith("cpu_")),
        "cpu_ metrics should be excluded");
  }

  @Test
  void regex_label_positive_matches_i1_and_i2_instances() {
    var cm = TestFixtures.buildCommonMocks();
    var evaluator =
        new ExpressionEvaluator(
            cm.client, cm.discovery, Executors.newFixedThreadPool(2), new MockStatsMerger());

    // Match any series with instance label i1 or i2
    var out = find(evaluator, "{instance=~\"i[12]\"}", cm.t0, cm.t3);

    assertFalse(out.isEmpty(), "expected series with instance i1/i2");
    var insts =
        out.stream()
            .map(s -> s.labels().tags().get("instance"))
            .collect(java.util.stream.Collectors.toSet());
    // We expect at least i1 and i2 present (cpu i1/i2, mem i1 exists in fixture)
    assertTrue(insts.contains("i1"), "missing instance i1");
    assertTrue(insts.contains("i2"), "missing instance i2");
  }

  @Test
  void regex_label_negative_excludes_i2_instance() {
    var cm = TestFixtures.buildCommonMocks();
    var evaluator =
        new ExpressionEvaluator(
            cm.client, cm.discovery, Executors.newFixedThreadPool(2), new MockStatsMerger());

    // Exclude instance exactly "i2"
    var out = find(evaluator, "{instance!~\"^i2$\"}", cm.t0, cm.t3);

    assertFalse(out.isEmpty(), "expected some series remaining after exclusion");
    // Ensure no series with instance=i2 is present
    assertTrue(
        out.stream().noneMatch(s -> "i2".equals(s.labels().tags().get("instance"))),
        "instance i2 should be excluded by negative regex");
  }
}
