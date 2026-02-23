/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.metrics.ch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.clickhouse.client.api.Client;
import com.google.gson.Gson;
import com.google.inject.Guice;
import com.google.inject.Injector;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.okapi.ch.CreateChTablesSpec;
import org.okapi.metrics.ch.template.ChMetricTemplateEngine;
import org.okapi.rest.metrics.query.GetMetricsRequest;
import org.okapi.rest.metrics.query.HistoQueryConfig;
import org.okapi.rest.metrics.query.METRIC_TYPE;
import org.okapi.testmodules.guice.TestChMetricsModule;

/** Sanity test for the JTE-based histogram query execution path. */
public class HistogramQueryProcessorJteTests {
  @TempDir java.nio.file.Path tempDir;

  private Injector injector;
  private Client client;
  private final Gson gson = new Gson();

  @BeforeEach
  void setup() {
    injector = Guice.createInjector(new TestChMetricsModule(tempDir.resolve("wal"), 16));
    client = injector.getInstance(Client.class);
    client.queryAll("DROP TABLE IF EXISTS okapi_metrics.histo_raw_samples");
    CreateChTablesSpec.migrate(client);
    client.queryAll("TRUNCATE TABLE IF EXISTS " + ChConstants.TBL_HISTOS);
  }

  @Test
  void fetchesSingleDeltaHistogram() throws Exception {
    var writer = injector.getInstance(ChWriter.class);
    var templateEngine = injector.getInstance(ChMetricTemplateEngine.class);
    var processor = new HistogramQueryProcessor(client, templateEngine);

    var resource = "svc-histo-" + UUID.randomUUID();
    var metric = "metric_h";
    var tags = Map.of("env", "dev");

    var buckets = new float[] {10.0f, 20.0f};
    var counts = new int[] {1, 2, 3};

    var row =
        ChHistoSampleInsertRow.builder()
            .resource(resource)
            .metric_name(metric)
            .tags(tags)
            .ts_start_ms(1_000)
            .ts_end_ms(2_000)
            .buckets(buckets)
            .counts(counts)
            .histo_type("DELTA")
            .build();
    writer.writeHistoSamplesBinary(List.of(row)).get();

    var req =
        GetMetricsRequest.builder()
            .svc(resource)
            .metric(metric)
            .tags(tags)
            .start(0)
            .end(5_000)
            .metricType(METRIC_TYPE.HISTO)
            .histoQueryConfig(
                HistoQueryConfig.builder().temporality(HistoQueryConfig.TEMPORALITY.DELTA).build())
            .build();

    var resp = processor.getHistoRes(req);
    assertNotNull(resp.getHistogramResponse());
    var histos = resp.getHistogramResponse().getHistograms();
    assertEquals(1, histos.size());
    assertEquals(List.of(1, 2, 3), histos.get(0).getCounts());
    assertEquals(List.of(10.0f, 20.0f), histos.get(0).getBuckets());
  }

  private static String formatTs(long epochMillis) {
    return Instant.ofEpochMilli(epochMillis)
        .atOffset(ZoneOffset.UTC)
        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"));
  }
}
