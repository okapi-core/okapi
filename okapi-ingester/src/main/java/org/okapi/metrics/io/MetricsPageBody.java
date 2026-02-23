package org.okapi.metrics.io;

import static org.okapi.primitives.MapSerializer.serializeMap;
import static org.okapi.primitives.OffsetTableSerializer.deserializeOffsetTable;
import static org.okapi.primitives.OffsetTableSerializer.serializeOffsetTable;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.okapi.io.NotEnoughBytesException;
import org.okapi.io.OkapiBufferDecoder;
import org.okapi.io.OkapiCheckedCountingWriter;
import org.okapi.io.StreamReadingException;
import org.okapi.metrics.primitives.GaugeBlock;
import org.okapi.metrics.primitives.HistoBlock;
import org.okapi.primitives.*;
import org.okapi.protos.metrics.METRIC_TYPE;
import org.okapi.protos.metrics.OffsetAndLen;

@Slf4j
public class MetricsPageBody implements ChecksumedSerializable {

  Map<String, GaugeBlock> gauges;
  Map<String, HistoBlock> histos;

  @Getter int approximateSize = 0;

  public MetricsPageBody() {
    this.gauges = new ConcurrentHashMap<>();
    this.histos = new ConcurrentHashMap<>();
  }

  public int updateGauge(String path, Long ts, float sample) {
    var size = gauges.computeIfAbsent(path, k -> new GaugeBlock()).updateStats(ts, sample);
    approximateSize += size;
    return size;
  }

  public int updateHistogram(String path, Long ts, Histogram histogram) {
    var size = histos.computeIfAbsent(path, k -> new HistoBlock()).updateHistogram(ts, histogram);
    approximateSize += size;
    return size;
  }

  public Optional<ReadOnlySketch> getSecondly(String path, Long ts, double[] ranks) {
    var gaugeBlock = gauges.get(path);
    return Optional.ofNullable(gaugeBlock.getSecondlyStat(ts / 1000, ranks));
  }

  public ReadOnlySketch getMinutely(String path, Long ts, double[] ranks) {
    var gaugeBlock = gauges.get(path);
    return gaugeBlock.getMinutelyStat(ts / 60000, ranks);
  }

  public ReadOnlySketch getHourly(String path, Long ts, double[] ranks) {
    var gaugeBlock = gauges.get(path);
    return gaugeBlock.getHourlyStat(ts / 3600000, ranks);
  }

  public Optional<ReadonlyHistogram> getHistogram(String path, Long ts) {
    var histoBlock = histos.get(path);
    if (histoBlock == null) {
      return Optional.empty();
    }
    return histoBlock.getHistogram(ts);
  }

  @Override
  public void fromChecksummedByteArray(byte[] bytes, int off, int len)
      throws StreamReadingException, IOException, NotEnoughBytesException {
    var decoder = new OkapiBufferDecoder();
    decoder.setBuffer(bytes, off, len);
    if (!decoder.isCrcMatch()) {
      throw new StreamReadingException("Checksum mismatch when reading MetricsPageBody");
    }
    var offsetTableLen = decoder.nextInt();
    var offsetTableStart = off + 4; // skip length of offset
    var blockStreamStart = (offsetTableStart + offsetTableLen);
    var decodedOffsetTable = deserializeOffsetTable(bytes, offsetTableStart, offsetTableLen);
    for (var entry : decodedOffsetTable.entrySet()) {
      var path = entry.getKey();
      var offsetAndLen = entry.getValue();
      var blockOffset = (4 + blockStreamStart) + (int) offsetAndLen.getOffset();
      var blockLen = offsetAndLen.getLen();
      switch (offsetAndLen.getMetricType()) {
        case METRIC_TYPE_GAUGE:
          var gaugeBlock = new GaugeBlock();
          gaugeBlock.fromChecksummedByteArray(bytes, blockOffset, blockLen);
          gauges.put(path, gaugeBlock);
          break;
        case METRIC_TYPE_HISTOGRAM:
          var histoBlock = new HistoBlock();
          histoBlock.fromChecksummedByteArray(bytes, blockOffset, blockLen);
          histos.put(path, histoBlock);
          break;
        default:
          throw new IOException(
              "Unknown metric type in offset table: " + offsetAndLen.getMetricType());
      }
    }
  }

  @Override
  public byte[] toChecksummedByteArray() throws IOException {
    var offsetTable = new HashMap<String, OffsetAndLen>();
    byte[] body;
    // use a simpler logic to write the calculations out
    {
      var blockStream = new ByteArrayOutputStream();
      var blockStreamWriter = new OkapiCheckedCountingWriter(blockStream);
      var gaugesOffsetTable =
          serializeMap(
              gauges.size(),
              gauges.entrySet().stream()
                  .map(entry -> new MapSerializer.KeyValuePair(entry.getKey(), entry.getValue())),
              METRIC_TYPE.METRIC_TYPE_GAUGE,
              blockStreamWriter);
      offsetTable.putAll(gaugesOffsetTable);
      // offset table is with relation to a specific stream not a shared stream
      var histosOffsetTable =
          serializeMap(
              histos.size(),
              histos.entrySet().stream()
                  .map(entry -> new MapSerializer.KeyValuePair(entry.getKey(), entry.getValue())),
              METRIC_TYPE.METRIC_TYPE_HISTOGRAM,
              blockStreamWriter);
      offsetTable.putAll(histosOffsetTable);
      body = blockStream.toByteArray();
    }
    byte[] offsetTableBytes = serializeOffsetTable(offsetTable);
    var os = new ByteArrayOutputStream();
    var writer = new OkapiCheckedCountingWriter(os);
    writer.writeBytesWithLenPrefix(offsetTableBytes);
    writer.writeBytesWithLenPrefix(body);
    writer.writeChecksum();
    var arr = os.toByteArray();
    return arr;
  }

  public MetricsPageBodySnapshot toSnapshot() {
    return new MetricsPageBodySnapshot(
        Collections.unmodifiableMap(gauges), Collections.unmodifiableMap(histos));
  }
}
