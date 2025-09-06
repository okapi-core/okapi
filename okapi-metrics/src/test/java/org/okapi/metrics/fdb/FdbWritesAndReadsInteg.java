package org.okapi.metrics.fdb;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.okapi.collections.OkapiLists;
import org.okapi.fixtures.ReadingGenerator;
import org.okapi.metrics.common.MetricPaths;
import org.okapi.metrics.common.pojo.Node;
import org.okapi.metrics.common.pojo.NodeState;
import org.okapi.metrics.pojos.AGG_TYPE;
import org.okapi.metrics.pojos.RES_TYPE;
import org.okapi.metrics.singletons.FdbSingletonFactory;
import org.okapi.metrics.stats.StatisticsFrozenException;
import org.okapi.rest.metrics.SubmitMetricsRequestInternal;

public class FdbWritesAndReadsInteg {

  Node node = new Node("test-node", "localhost", NodeState.METRICS_CONSUMPTION_START);
  public String METRIC = "metric_" + System.currentTimeMillis();
  public String TENANT_ID = "tenant_id";
  SubmitMetricsRequestInternal.SubmitMetricsRequestInternalBuilder prototype;
  public static final Map<String, String> TAGS =
      Map.of(
          "key1", "value1",
          "key2", "value2");
  FdbSingletonFactory fdbSingletonFactory;

  @BeforeEach
  public void setup() {
    fdbSingletonFactory = new FdbSingletonFactory();
    prototype =
        SubmitMetricsRequestInternal.builder().tenantId(TENANT_ID).metricName(METRIC).tags(TAGS);
  }

  @ParameterizedTest
  @ValueSource(strings = {"s", "m", "h"})
  public void testWriteSinglePoint(String res)
      throws InterruptedException, StatisticsFrozenException {
    var resType = RES_TYPE.parse(res);
    assertNotNull(resType.isPresent());
    var messageBox = fdbSingletonFactory.messageBox(node);
    var writer = fdbSingletonFactory.fdbWriter(node);
    var reading =
        new ReadingGenerator(Duration.of(100, ChronoUnit.MILLIS), 2).populateRandom(0.f, 1.f);
    var request =
        prototype
            .ts(OkapiLists.toLongArray(reading.getTimestamps()))
            .values(OkapiLists.toFloatArray(reading.getValues()))
            .build();
    messageBox.push(request);
    writer.writeOnce();
    assertTrue(messageBox.isEmpty());

    // get the reader
    var path = MetricPaths.convertToPath(TENANT_ID, METRIC, TAGS);
    var reader = fdbSingletonFactory.fdbTsReader(node);
    var startTime = reading.getTimestamps().stream().reduce(Math::min).get();
    var endTime = reading.getTimestamps().stream().reduce(Math::max).get();
    var scanResult = reader.scan(path, startTime, endTime, AGG_TYPE.AVG, resType.get());

    // check the reading
    var reduction = reading.avgReduction(resType.get());
    assertEquals(reduction.getTimestamp(), scanResult.getTimestamps());
    assertEquals(reduction.getValues(), scanResult.getValues());
  }
}
