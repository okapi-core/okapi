package org.okapi.promql.eval;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.junit.jupiter.api.Test;
import org.okapi.promql.MockSeriesDiscovery;
import org.okapi.promql.MockStatsMerger;
import org.okapi.promql.MockTimeSeriesClient;
import org.okapi.promql.eval.VectorData.Labels;
import org.okapi.promql.eval.VectorData.SeriesId;
import org.okapi.promql.parser.PromQLLexer;
import org.okapi.promql.parser.PromQLParser;

public class HistogramQuantileMergeTest {

  @Test
  void histogramQuantile_mergesAcrossInstances() throws Exception {
    var client = new MockTimeSeriesClient();
    long step = 60_000L;
    long t0 = 1_700_000_000_000L;
    long t1 = t0 + step;
    long t3 = t1 + 2 * step;

    var tagsI1 = Map.of("job", "api", "instance", "i1");
    var tagsI2 = Map.of("job", "api", "instance", "i2");

    var sI1 = new SeriesId("latency_histo", new Labels(tagsI1));
    var sI2 = new SeriesId("latency_histo", new Labels(tagsI2));

    // Two histograms with different schemas
    client.putHisto("latency_histo", tagsI1, t0, t3, List.of(100f, 200f), List.of(10, 10, 0));
    client.putHisto("latency_histo", tagsI2, t0, t3, List.of(150f, 250f), List.of(0, 10, 10));

    var discovery = new MockSeriesDiscovery(List.of(sI1, sI2));
    var exec = Executors.newFixedThreadPool(2);
    var merger = new MockStatsMerger();
    var evaluator = new ExpressionEvaluator(client, discovery, exec, merger);

    String promql = "histogram_quantile(0.5, latency_histo[2m])";
    var parser =
        new PromQLParser(new CommonTokenStream(new PromQLLexer(CharStreams.fromString(promql))));

    var res = evaluator.evaluate(promql, t1, t3, step, parser);
    assertEquals(ValueType.INSTANT_VECTOR, res.type());
    var iv = (InstantVectorResult) res;

    // Expect a single merged series key (instance stripped) with job=api only
    var distinct = new java.util.LinkedHashSet<SeriesId>();
    for (var s : iv.data()) distinct.add(s.series());
    assertEquals(1, distinct.size());
    var sid = distinct.iterator().next();
    assertEquals("latency_histo", sid.metric());
    assertNull(sid.labels().tags().get("instance"));
    assertEquals("api", sid.labels().tags().get("job"));
  }
}
