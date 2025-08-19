package org.okapi.metrics.scanning;

import com.google.common.primitives.Longs;
import org.okapi.exceptions.ExceptionUtils;
import org.okapi.metrics.CondensedReading;
import org.okapi.metrics.io.OkapiIo;
import org.okapi.metrics.io.StreamReadingException;
import org.okapi.metrics.stats.KllSketchRestorer;
import org.okapi.metrics.stats.Statistics;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;

@Slf4j
public class HourlyCheckpointScanner {

  public Map<String, List<Long>> getMd(ByteRangeScanner scanner)
      throws IOException, StreamReadingException, ExecutionException, EmptyFileException {
    var tot = scanner.totalBytes();
    if (tot < 8) {
      throw new EmptyFileException();
    }
    var mdOffset = Longs.fromByteArray(scanner.getRange(tot - 8, 8));
    var mdBytes = tot - mdOffset - 8;
    if (mdBytes > Integer.MAX_VALUE) {
      throw new IllegalArgumentException(
          "Huge metadata is not supported. tried to read " + mdBytes + " bytes");
    }
    var md = scanner.getRange(mdOffset, (int) mdBytes);
    return parseMetadata(md);
  }

  public Set<String> listMetrics(ByteRangeScanner rangeScanner)
      throws IOException, StreamReadingException, ExecutionException {
    try {
      var parsed = getMd(rangeScanner);
      return parsed.keySet();
    } catch (EmptyFileException e) {
      log.error("Found empty where not expected. {}", ExceptionUtils.debugFriendlyMsg(e));
    }
    return Collections.emptySet();
  }

  public CondensedReading secondly(
      ByteRangeScanner sc, String metricPath, Map<String, List<Long>> md)
      throws StreamReadingException, IOException, ExecutionException {
    var offs = md.get(metricPath);
    var secOff = offs.get(0);
    var minOff = offs.get(1);
    var nSecBytes = minOff - secOff;
    return readBlock(sc, secOff, (int) nSecBytes);
  }

  public CondensedReading minutely(
      ByteRangeScanner sc, String metricPath, Map<String, List<Long>> md)
      throws StreamReadingException, IOException, ExecutionException {
    var offs = md.get(metricPath);
    var minOff = offs.get(1);
    var hrOff = offs.get(2);
    var nSecBytes = hrOff - minOff;
    return readBlock(sc, minOff, (int) nSecBytes);
  }

  public Statistics hourly(ByteRangeScanner sc, String metricPath, Map<String, List<Long>> md)
      throws StreamReadingException, IOException, ExecutionException {
    var offs = md.get(metricPath);
    var hrOff = offs.get(2);
    var endOff = offs.get(3);
    var nb = (int) (endOff - hrOff);
    var bis = new ByteArrayInputStream(sc.getRange(hrOff, nb));
    var b = OkapiIo.readBytes(bis);
    return Statistics.deserialize(b, new KllSketchRestorer());
  }

  protected CondensedReading readBlock(ByteRangeScanner sc, long from, int nb)
      throws StreamReadingException, IOException, ExecutionException {
    var block = sc.getRange(from, nb);
    var bis = new ByteArrayInputStream(block);
    var nPts = OkapiIo.readInt(bis);
    var secs = new ArrayList<Integer>();
    var vals = new ArrayList<Statistics>();
    for (int i = 0; i < nPts; i++) {
      var s = OkapiIo.readInt(bis);
      secs.add(s);
      var b = OkapiIo.readBytes(bis);
      var stat = Statistics.deserialize(b, new KllSketchRestorer());
      vals.add(stat);
    }
    return new CondensedReading(secs, vals);
  }

  public Map<String, List<Long>> parseMetadata(byte[] md)
      throws StreamReadingException, IOException {
    var bis = new ByteArrayInputStream(md);
    var nPaths = OkapiIo.readInt(bis);
    var offs = new HashMap<String, List<Long>>();
    for (int i = 0; i < nPaths; i++) {
      var k = OkapiIo.readString(bis);
      var secOff = OkapiIo.readLong(bis);
      var minOff = OkapiIo.readLong(bis);
      var hrOff = OkapiIo.readLong(bis);
      var endOff = OkapiIo.readLong(bis);
      offs.put(k, Collections.unmodifiableList(Arrays.asList(secOff, minOff, hrOff, endOff)));
    }
    return offs;
  }
}
