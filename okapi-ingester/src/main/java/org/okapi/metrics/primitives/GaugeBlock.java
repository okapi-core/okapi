package org.okapi.metrics.primitives;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.okapi.io.NotEnoughBytesException;
import org.okapi.io.OkapiBufferDecoder;
import org.okapi.io.OkapiCheckedCountingWriter;
import org.okapi.io.StreamReadingException;
import org.okapi.metrics.pojos.RES_TYPE;
import org.okapi.primitives.ChecksumedSerializable;
import org.okapi.primitives.GaugeSketch;
import org.okapi.primitives.ReadOnlySketch;

@Slf4j
public class GaugeBlock implements ChecksumedSerializable {

  Map<Long, GaugeSketch> secondly = new ConcurrentHashMap<>();
  Map<Long, GaugeSketch> minutely = new ConcurrentHashMap<>();
  Map<Long, GaugeSketch> hourly = new ConcurrentHashMap<>();

  public ReadOnlySketch getSecondlyStat(Long secondlyBlock, double[] ranks) {
    var sketch = secondly.get(secondlyBlock);
    if (sketch == null) {
      return null;
    }
    return sketch.getWithQuantiles();
  }

  public ReadOnlySketch getMinutelyStat(Long minutelyBlock, double[] ranks) {
    var sketch = minutely.get(minutelyBlock);
    if (sketch == null) {
      return null;
    }
    return sketch.getWithQuantiles();
  }

  public ReadOnlySketch getHourlyStat(Long hourlyBlock, double[] ranks) {
    var sketch = hourly.get(hourlyBlock);
    if (sketch == null) {
      return null;
    }
    return sketch.getWithQuantiles();
  }

  public Optional<ReadOnlySketch> getStat(Long block, RES_TYPE resType, double[] ranks) {
    return switch (resType) {
      case SECONDLY -> Optional.ofNullable(getSecondlyStat(block, ranks));
      case MINUTELY -> Optional.ofNullable(getMinutelyStat(block, ranks));
      case HOURLY -> Optional.ofNullable(getHourlyStat(block, ranks));
      default -> {
        log.warn("Unknown RES_TYPE {} when getting GaugeStat", resType);
        yield null;
      }
    };
  }

  public int updateStats(Long ts, float sample) {
    return updateSecondlyStats(ts, sample)
        + updateMinutelyStats(ts, sample)
        + updateHourlyStats(ts, sample);
  }

  int updateSecondlyStats(Long ts, float sample) {
    var secondlyBlock = ts / 1000;
    var sketch = secondly.computeIfAbsent(secondlyBlock, k -> new GaugeSketch());
    sketch.update(sample);
    return sketch.byteSize();
  }

  int updateMinutelyStats(Long ts, float sample) {
    var minutelyBlock = ts / 60_000;
    var sketch = minutely.computeIfAbsent(minutelyBlock, k -> new GaugeSketch());
    sketch.update(sample);
    return sketch.byteSize();
  }

  int updateHourlyStats(Long ts, float sample) {
    var hourlyBlock = ts / 3600_000;
    var sketch = hourly.computeIfAbsent(hourlyBlock, k -> new GaugeSketch());
    sketch.update(sample);
    return sketch.byteSize();
  }

  @Override
  public void fromChecksummedByteArray(byte[] bytes, int off, int len)
      throws StreamReadingException, IOException, NotEnoughBytesException {
    var decoder = new OkapiBufferDecoder();
    decoder.setBuffer(bytes, off, len);
    if (!decoder.isCrcMatch()) {
      throw new StreamReadingException("CRC mismatch when reading GaugeBlock");
    }
    byte[] flags = decoder.nextBytesNoLenPrefix(2);
    if (flags[0] != BlockFlags.GAUGE[0] || flags[1] != BlockFlags.GAUGE[1]) {
      throw new StreamReadingException("Invalid block flags when reading Gauge block");
    }
    var secondlySize = decoder.nextInt();
    for (int i = 0; i < secondlySize; i++) {
      var key = decoder.nextLong();
      var valueBytes = decoder.nextBytesLenPrefix();
      var gaugeSketch = new GaugeSketch();
      gaugeSketch.fromByteArray(valueBytes, 0, valueBytes.length);
      secondly.put(key, gaugeSketch);
    }

    var minutelySize = decoder.nextInt();
    for (int i = 0; i < minutelySize; i++) {
      var key = decoder.nextLong();
      var valueBytes = decoder.nextBytesLenPrefix();
      var gaugeSketch = new GaugeSketch();
      gaugeSketch.fromByteArray(valueBytes, 0, valueBytes.length);
      minutely.put(key, gaugeSketch);
    }

    var hourlySize = decoder.nextInt();
    for (int i = 0; i < hourlySize; i++) {
      var key = decoder.nextLong();
      var valueBytes = decoder.nextBytesLenPrefix();
      var gaugeSketch = new GaugeSketch();
      gaugeSketch.fromByteArray(valueBytes, 0, valueBytes.length);
      hourly.put(key, gaugeSketch);
    }
  }

  @Override
  public byte[] toChecksummedByteArray() throws IOException {
    var os = new ByteArrayOutputStream();
    var writer = new OkapiCheckedCountingWriter(os);
    writer.writeBytesWithoutLenPrefix(BlockFlags.GAUGE);
    writer.writeInt(secondly.size());
    for (var entry : secondly.entrySet()) {
      writer.writeLong(entry.getKey());
      writer.writeBytesWithLenPrefix(entry.getValue().toByteArray());
    }
    writer.writeInt(minutely.size());
    for (var entry : minutely.entrySet()) {
      writer.writeLong(entry.getKey());
      writer.writeBytesWithLenPrefix(entry.getValue().toByteArray());
    }
    writer.writeInt(hourly.size());
    for (var entry : hourly.entrySet()) {
      writer.writeLong(entry.getKey());
      writer.writeBytesWithLenPrefix(entry.getValue().toByteArray());
    }
    writer.writeChecksum();
    return os.toByteArray();
  }
}
