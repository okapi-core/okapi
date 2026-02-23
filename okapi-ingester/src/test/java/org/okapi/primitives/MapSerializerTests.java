/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.primitives;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import org.junit.jupiter.api.Test;
import org.okapi.io.NotEnoughBytesException;
import org.okapi.io.OkapiBufferDecoder;
import org.okapi.io.OkapiCheckedCountingWriter;
import org.okapi.io.StreamReadingException;
import org.okapi.metrics.primitives.GaugeBlock;
import org.okapi.protos.metrics.METRIC_TYPE;

public class MapSerializerTests {

  @Test
  void testSerializeMap_singleBlock()
      throws IOException, StreamReadingException, NotEnoughBytesException {
    var gaugeBlock = new GaugeBlock();
    gaugeBlock.updateStats(1000L, 42.0f);
    gaugeBlock.updateStats(2000L, 43.0f);
    gaugeBlock.updateStats(3000L, 44.0f);
    var map = new HashMap<String, GaugeBlock>();
    map.put("gauge1", gaugeBlock);

    var os = new ByteArrayOutputStream();
    var writer = new OkapiCheckedCountingWriter(os);
    var offsetTable =
        MapSerializer.serializeMap(
            map.size(),
            map.entrySet().stream()
                .map(e -> new MapSerializer.KeyValuePair(e.getKey(), e.getValue())),
            org.okapi.protos.metrics.METRIC_TYPE.METRIC_TYPE_GAUGE,
            writer);

    var output = os.toByteArray();
    var reader = new OkapiBufferDecoder();
    var offsetEntry = offsetTable.get("gauge1");
    reader.setBuffer(output, (int) offsetEntry.getOffset(), offsetEntry.getLen());
    assertTrue(reader.isCrcMatch()); // skip CRC check for test

    var decoded = new GaugeBlock();
    decoded.fromChecksummedByteArray(output, (int) offsetEntry.getOffset(), offsetEntry.getLen());

    assertEquals(42.0, decoded.getSecondlyStat(1L, new double[] {}).getMean());
    assertEquals(43.0, decoded.getSecondlyStat(2L, new double[] {}).getMean());
    assertEquals(44.0, decoded.getSecondlyStat(3L, new double[] {}).getMean());
  }

  @Test
  void testGaugeBlockSerialization_emptyBlock()
      throws IOException, StreamReadingException, NotEnoughBytesException {
    var gaugeBlock = new GaugeBlock();

    var serialized = gaugeBlock.toChecksummedByteArray();
    var deserializedBlock = new GaugeBlock();
    deserializedBlock.fromChecksummedByteArray(serialized, 0, serialized.length);

    // Verify that no stats exist
    var secondlyStat = deserializedBlock.getSecondlyStat(0L, new double[] {});
    assertNull(secondlyStat);
  }

  @Test
  void testMapSerialization_mixedBlocks()
      throws IOException, StreamReadingException, NotEnoughBytesException {
    var map = new HashMap<String, ChecksumedSerializable>();
    var gaugeBlock = new GaugeBlock();
    gaugeBlock.updateStats(1000L, 42.0f);
    map.put("gauge1", gaugeBlock);
    var gaugeBlock2 = new GaugeBlock();
    gaugeBlock2.updateStats(2000L, 42.0f);
    map.put("gauge2", gaugeBlock2);
    var os = new ByteArrayOutputStream();
    var writer = new OkapiCheckedCountingWriter(os);
    var offsetTable =
        MapSerializer.serializeMap(
            map.size(),
            map.entrySet().stream()
                .map(e -> new MapSerializer.KeyValuePair(e.getKey(), e.getValue())),
            METRIC_TYPE.METRIC_TYPE_GAUGE,
            writer);

    var decodedGauge1 = new GaugeBlock();
    var offsetEntry1 = offsetTable.get("gauge1");
    decodedGauge1.fromChecksummedByteArray(
        os.toByteArray(), (int) offsetEntry1.getOffset(), offsetEntry1.getLen());
    assertEquals(42.0, decodedGauge1.getSecondlyStat(1L, new double[] {}).getMean());

    var decodedGauge2 = new GaugeBlock();
    var offsetEntry2 = offsetTable.get("gauge2");
    decodedGauge2.fromChecksummedByteArray(
        os.toByteArray(), (int) offsetEntry2.getOffset(), offsetEntry2.getLen());
    assertEquals(42.0, decodedGauge2.getSecondlyStat(2L, new double[] {}).getMean());
  }
}
