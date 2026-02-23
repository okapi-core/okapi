/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.metrics;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.okapi.fixtures.GaugeGenerator;
import org.okapi.io.NotEnoughBytesException;
import org.okapi.io.StreamReadingException;
import org.okapi.metrics.pojos.RES_TYPE;
import org.okapi.metrics.primitives.GaugeBlock;

@Slf4j
public class GaugeBlockTests {

  @Test
  void testGaugeBlock_singleStat()
      throws IOException, StreamReadingException, NotEnoughBytesException {
    var block = new GaugeBlock();
    block.updateStats(1L, 0.01f);

    var serialized = block.toChecksummedByteArray();
    var deserializedBlock = new GaugeBlock();
    deserializedBlock.fromChecksummedByteArray(serialized, 0, serialized.length);

    var secondlyStat = deserializedBlock.getSecondlyStat(0L, new double[] {0.0});
    assertEquals(0.01f, secondlyStat.getMean());
    assertEquals(1.0f, secondlyStat.getCount());
    assertEquals(0.01f, secondlyStat.getQuantile(0.0));
  }

  @Test
  void testGaugeBlock_twoStats() {
    var block = new GaugeBlock();
    block.updateStats(1L, 0.01f);
    block.updateStats(1L, 0.03f);

    var secondlyStat = block.getSecondlyStat(0L, new double[] {0.0, 0.5, 1.0});
    assertEquals(0.02f, secondlyStat.getMean());
    assertEquals(2.0f, secondlyStat.getCount());
    assertEquals(0.01f, secondlyStat.getQuantile(0.0));
    assertEquals(0.01f, secondlyStat.getQuantile(0.5));
    assertEquals(0.03f, secondlyStat.getQuantile(1.0));
  }

  @Test
  void testGaugeBlock_threeStats() {
    var block = new GaugeBlock();
    block.updateStats(1L, 0.01f);
    block.updateStats(1L, 0.03f);
    block.updateStats(1L, 0.02f);

    var secondlyStat = block.getSecondlyStat(0L, new double[] {0.0, 0.5, 1.0});
    assertEquals(0.02f, secondlyStat.getMean());
    assertEquals(3.0f, secondlyStat.getCount());
    assertEquals(0.01f, secondlyStat.getQuantile(0.0));
    assertEquals(0.02f, secondlyStat.getQuantile(0.5));
    assertEquals(0.03f, secondlyStat.getQuantile(1.0));
  }

  @Test
  void testGaugeBlock_multipleTimeBlocks()
      throws IOException, StreamReadingException, NotEnoughBytesException {
    var block = new GaugeBlock();
    block.updateStats(1000L, 0.01f); // secondly block 1
    block.updateStats(2000L, 0.03f); // secondly block 2

    var serialized = block.toChecksummedByteArray();
    var deserializedBlock = new GaugeBlock();
    deserializedBlock.fromChecksummedByteArray(serialized, 0, serialized.length);

    var secondlyStatBlock1 = deserializedBlock.getSecondlyStat(1L, new double[] {0.0});
    assertEquals(0.01f, secondlyStatBlock1.getMean());
    assertEquals(1.0f, secondlyStatBlock1.getCount());
    assertEquals(0.01f, secondlyStatBlock1.getQuantile(0.0));

    var secondlyStatBlock2 = deserializedBlock.getSecondlyStat(2L, new double[] {0.0});
    assertEquals(0.03f, secondlyStatBlock2.getMean());
    assertEquals(1.0f, secondlyStatBlock2.getCount());
    assertEquals(0.03f, secondlyStatBlock2.getQuantile(0.0));
  }

  @Test
  void testGaugeBlock_minutelyStats() {
    var block = new GaugeBlock();
    block.updateStats(60_000L, 0.01f); // minutely block 1
    block.updateStats(120_000L, 0.03f); // minutely block 2

    var minutelyStatBlock1 = block.getMinutelyStat(1L, new double[] {0.0});
    assertEquals(0.01f, minutelyStatBlock1.getMean());
    assertEquals(1.0f, minutelyStatBlock1.getCount());
    assertEquals(0.01f, minutelyStatBlock1.getQuantile(0.0));

    var minutelyStatBlock2 = block.getMinutelyStat(2L, new double[] {0.0});
    assertEquals(0.03f, minutelyStatBlock2.getMean());
    assertEquals(1.0f, minutelyStatBlock2.getCount());
    assertEquals(0.03f, minutelyStatBlock2.getQuantile(0.0));
  }

  @Test
  void testGaugeblock_minutelyMoreThanOnePerMinute() {
    var block = new GaugeBlock();
    block.updateStats(60_000L, 0.01f); // minutely block 1
    block.updateStats(61_000L, 0.03f); // minutely block 1

    var minutelyStatBlock1 = block.getMinutelyStat(1L, new double[] {0.0, 0.5, 1.0});
    assertEquals(0.02f, minutelyStatBlock1.getMean());
    assertEquals(2.0f, minutelyStatBlock1.getCount());
  }

  @Test
  void testGaugeBlock_noData() {
    var block = new GaugeBlock();

    var secondlyStat = block.getSecondlyStat(0L, new double[] {0.0});
    assertEquals(null, secondlyStat);
  }

  @Test
  void testGaugeBlock_quantiles()
      throws IOException, StreamReadingException, NotEnoughBytesException {
    var randomGenerator = GaugeGenerator.create(2, 0.1f, 0.2f, 0.999);
    var values = randomGenerator.getValues();
    var ts = randomGenerator.getTimestamps();
    var deserializedBlock = new GaugeBlock();
    {
      var gauge = new GaugeBlock();
      for (int i = 0; i < values.size(); i++) {
        gauge.updateStats(ts.get(i), values.get(i));
      }
      var serialized = gauge.toChecksummedByteArray();
      deserializedBlock.fromChecksummedByteArray(serialized, 0, serialized.length);
    }

    // test secondly secondlyReduction
    {
      var secondlyReduction = randomGenerator.avgReduction(RES_TYPE.SECONDLY);
      for (int i = 0; i < secondlyReduction.getTimestamp().size(); i++) {
        var timestamp = secondlyReduction.getTimestamp().get(i);
        var value = secondlyReduction.getValues().get(i);
        var secondBlock = timestamp / 1000;
        var secondlyStat =
            deserializedBlock.getSecondlyStat(secondBlock, new double[] {0.0, 0.5, 1.0});
        assertEquals(secondlyStat.getMean(), value, 0.0001f);
      }
    }

    // first

    // test minutely secondlyReduction
    {
      var minutely = randomGenerator.avgReduction(RES_TYPE.MINUTELY);

      for (int i = 0; i < minutely.getTimestamp().size(); i++) {
        var timestamp = minutely.getTimestamp().get(i);
        var expectedValue = minutely.getValues().get(i);
        var minutelyBlock = timestamp / 60_000;
        var minutelyStat =
            deserializedBlock.getMinutelyStat(minutelyBlock, new double[] {0.0, 0.5, 1.0});
        assertEquals(minutelyStat.getMean(), expectedValue, 0.0001f);
      }
    }

    {
      var hourly = randomGenerator.avgReduction(RES_TYPE.HOURLY);
      for (int i = 0; i < hourly.getTimestamp().size(); i++) {
        var timestamp = hourly.getTimestamp().get(i);
        var expectedValue = hourly.getValues().get(i);
        var hourlyBlock = timestamp / 3_600_000;
        var hourlyStat = deserializedBlock.getHourlyStat(hourlyBlock, new double[] {0.0, 0.5, 1.0});
        assertEquals(hourlyStat.getMean(), expectedValue, 0.0001f);
      }
    }
  }
}
