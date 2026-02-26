/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.traces.ch.reds;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.clickhouse.client.api.Client;
import com.google.inject.Guice;
import com.google.inject.Injector;
import io.opentelemetry.proto.trace.v1.Status;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.okapi.ch.CreateChTablesSpec;
import org.okapi.chtest.ChTestOnlyUtils;
import org.okapi.metrics.ch.ChConstants;
import org.okapi.metrics.pojos.RES_TYPE;
import org.okapi.rest.traces.TimestampFilter;
import org.okapi.rest.traces.red.RedMetrics;
import org.okapi.rest.traces.red.ServiceRedRequest;
import org.okapi.rest.traces.red.ServiceRedResponse;
import org.okapi.testmodules.guice.TestChTracesModule;
import org.okapi.timeutils.TimeUtils;
import org.okapi.traces.OtelTestFactory;
import org.okapi.traces.ch.ChTracesIngester;
import org.okapi.traces.ch.ChTracesWalConsumerDriver;

public class ChRedIntegrationAggregateTests {

  @TempDir Path tempDir;

  private Injector injector;
  private Client client;
  private ChRedQueryService redQueryService;
  private long baseMs;
  private OtelTestFactory otelTestFactory;

  @BeforeEach
  void setup() throws Exception {
    injector = Guice.createInjector(new TestChTracesModule(tempDir.resolve("wal"), 16));
    client = injector.getInstance(Client.class);
    CreateChTablesSpec.migrate(client);
    ChTestOnlyUtils.truncateTable(client, ChConstants.TBL_SERVICE_RED_EVENTS);
    redQueryService = injector.getInstance(ChRedQueryService.class);
    baseMs = TimeUtils.roundToNearestHour(1_700_000_000_000L);
    otelTestFactory = new OtelTestFactory();
    ingestCorpus();
  }

  @Test
  void aggregateRedQueryIncludesAllSections() {
    var response = queryRed();
    assertNotNull(response);
    assertEquals("svc-agg", response.getService());
    assertNotNull(response.getServiceRed());
    assertNotNull(response.getPeerReds());
    assertNotNull(response.getServiceOpReds());
    assertEquals(1, response.getServiceOpReds().size());
    assertEquals(1, response.getPeerReds().size());
    assertEquals(1, response.getTotalDetectedOps());

    var serviceRed = response.getServiceRed();
    assertEquals(
        RedMetrics.of(
            List.of(baseMs + 1_000L),
            List.of(2L),
            List.of(1L),
            List.of(200.0)),
        serviceRed);
  }

  private void ingestCorpus() throws Exception {
    var ingester = injector.getInstance(ChTracesIngester.class);
    var driver = injector.getInstance(ChTracesWalConsumerDriver.class);

    var spans =
        List.of(
            otelTestFactory.span(
                "op.checkout", "svc-peer", baseMs + 1_000L, 200L, Status.StatusCode.STATUS_CODE_OK),
            otelTestFactory.span(
                "op.checkout",
                "svc-peer",
                baseMs + 1_000L,
                300L,
                Status.StatusCode.STATUS_CODE_ERROR));
    var request =
        otelTestFactory.buildRequest(
            List.of(otelTestFactory.resourceSpans("svc-agg", spans)));
    ingester.ingest(request);
    driver.onTick();
  }

  private ServiceRedResponse queryRed() {
    var filter =
        TimestampFilter.builder()
            .tsStartNanos(TimeUtils.millisToNanos(baseMs))
            .tsEndNanos(TimeUtils.millisToNanos(baseMs + 10_000L))
            .build();
    var request =
        ServiceRedRequest.builder()
            .service("svc-agg")
            .resType(RES_TYPE.SECONDLY)
            .timestampFilter(filter)
            .build();
    return redQueryService.queryRed(request);
  }

}
