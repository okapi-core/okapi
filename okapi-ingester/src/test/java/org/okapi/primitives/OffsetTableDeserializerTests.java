package org.okapi.primitives;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.okapi.primitives.OffsetTableSerializer.deserializeOffsetTable;
import static org.okapi.primitives.OffsetTableSerializer.serializeOffsetTable;

import java.io.IOException;
import java.util.HashMap;
import org.junit.jupiter.api.Test;
import org.okapi.io.NotEnoughBytesException;
import org.okapi.protos.metrics.METRIC_TYPE;
import org.okapi.protos.metrics.OffsetAndLen;

public class OffsetTableDeserializerTests {

  @Test
  void testSerializeOffsetTable_singleEntry() throws IOException, NotEnoughBytesException {
    var offsetTable = new HashMap<String, OffsetAndLen>();
    offsetTable.put(
        "metric1",
        OffsetAndLen.newBuilder()
            .setMetricType(METRIC_TYPE.METRIC_TYPE_HISTOGRAM)
            .setOffset(0L)
            .setLen(100)
            .build());

    var serialized = serializeOffsetTable(offsetTable);
    var deserialized = deserializeOffsetTable(serialized, 0, serialized.length);
    assertEquals(1, deserialized.size());
    assertEquals(0L, deserialized.get("metric1").getOffset());
    assertEquals(100, deserialized.get("metric1").getLen());
    assertEquals(METRIC_TYPE.METRIC_TYPE_HISTOGRAM, deserialized.get("metric1").getMetricType());
  }

  @Test
  void testSerializeOffsetTable_multipleEntry() throws IOException, NotEnoughBytesException {
    var offsetTable = new HashMap<String, OffsetAndLen>();
    offsetTable.put(
        "metric1",
        OffsetAndLen.newBuilder()
            .setMetricType(METRIC_TYPE.METRIC_TYPE_GAUGE)
            .setOffset(0L)
            .setLen(100)
            .build());
    offsetTable.put(
        "metric2",
        OffsetAndLen.newBuilder()
            .setMetricType(METRIC_TYPE.METRIC_TYPE_HISTOGRAM)
            .setOffset(10L)
            .setLen(10)
            .build());

    var serialized = serializeOffsetTable(offsetTable);
    var deserialized = deserializeOffsetTable(serialized, 0, serialized.length);
    assertEquals(2, deserialized.size());
    assertEquals(0L, deserialized.get("metric1").getOffset());
    assertEquals(100, deserialized.get("metric1").getLen());
    assertEquals(METRIC_TYPE.METRIC_TYPE_GAUGE, deserialized.get("metric1").getMetricType());
    assertEquals(10L, deserialized.get("metric2").getOffset());
    assertEquals(10, deserialized.get("metric2").getLen());
    assertEquals(METRIC_TYPE.METRIC_TYPE_HISTOGRAM, deserialized.get("metric2").getMetricType());
  }
}
