package org.okapi.metrics.io;

import static org.okapi.serialization.BloomSerializer.*;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.okapi.abstractio.TrigramUtil;
import org.okapi.io.NotEnoughBytesException;
import org.okapi.io.OkapiBufferDecoder;
import org.okapi.io.OkapiCheckedCountingWriter;
import org.okapi.pages.AbstractTimeBlockMetadata;

public class MetricsPageMetadata extends AbstractTimeBlockMetadata {
  BloomFilter<Integer> metricNameTrigrams;
  BloomFilter<Integer> tagPatternTrigrams;

  public MetricsPageMetadata(int expectedInsertions, double fpp) {
    this.metricNameTrigrams = BloomFilter.create(Funnels.integerFunnel(), expectedInsertions, fpp);
    this.tagPatternTrigrams = BloomFilter.create(Funnels.integerFunnel(), expectedInsertions, fpp);
  }

  public List<Integer> getMetricNameTrigrams(String metricPath) {
    return TrigramUtil.extractAsciiTrigramIndices(metricPath);
  }

  public boolean maybeContainsMetricName(int trigram) {
    return this.metricNameTrigrams.mightContain(trigram);
  }

  public boolean maybeContainsTagPattern(int trigram) {
    return this.tagPatternTrigrams.mightContain(trigram);
  }

  public boolean maybeContainsPath(String path, Map<String, String> tags) {
    var pathTrigrams = getMetricNameTrigrams(path);
    for (var trigram : pathTrigrams) {
      if (!maybeContainsMetricName(trigram)) {
        return false;
      }
    }
    var tagtrigrams = getTagPatternTrigrams(tags);
    for (var trigram : tagtrigrams) {
      if (!maybeContainsTagPattern(trigram)) {
        return false;
      }
    }
    return true;
  }

  public List<Integer> getTagPatternTrigrams(Map<String, String> tags) {
    var trigrams = new java.util.ArrayList<Integer>();
    if (tags == null) return trigrams;
    for (var tag : tags.entrySet()) {
      var tagName = tag.getKey();
      var tagValue = tag.getValue();
      var tagTrigrams = TrigramUtil.extractAsciiTrigramIndices(tagName);
      var valTrigrams = TrigramUtil.extractAsciiTrigramIndices(tagValue);
      var lastCharTag = tagName.charAt(tagName.length() - 1);
      var lastCharValue = tagValue.charAt(tagValue.length() - 1);
      var combinedTrigram = TrigramUtil.getTrigramIndex(lastCharTag, ' ', lastCharValue);
      trigrams.addAll(tagTrigrams);
      trigrams.addAll(valTrigrams);
      trigrams.add(combinedTrigram);
    }
    return trigrams;
  }

  public void addPathMetadata(String metricPath) {
    var trigrams = TrigramUtil.extractAsciiTrigramIndices(metricPath);
    for (var trigram : trigrams) {
      metricNameTrigrams.put(trigram);
    }
  }

  public void addTagPatternMetadata(Map<String, String> tags) {
    var trigrams = getTagPatternTrigrams(tags);
    for (var trigram : trigrams) {
      tagPatternTrigrams.put(trigram);
    }
  }

  public byte[] toByteArray() throws IOException {
    var os = new ByteArrayOutputStream();
    var writer = new OkapiCheckedCountingWriter(os);
    writer.writeLong(this.getMaxLsn());
    writer.writeLong(this.getTsStart());
    writer.writeLong(this.getTsEnd());
    writer.writeBytesWithLenPrefix(serializeBloom(metricNameTrigrams));
    writer.writeBytesWithLenPrefix(serializeBloom(tagPatternTrigrams));
    writer.writeChecksum();
    return os.toByteArray();
  }

  public void fromDecoder(OkapiBufferDecoder decoder) throws NotEnoughBytesException, IOException {
    if (!decoder.isCrcMatch()) {
      throw new IOException("Checksum mismatch when reading MetricsPageMetadata");
    }
    setMaxLsn(decoder.nextLong());
    setTsStart(decoder.nextLong());
    setTsEnd(decoder.nextLong());
    this.metricNameTrigrams = deserializeIntBloom(decoder.nextBytesLenPrefix());
    this.tagPatternTrigrams = deserializeIntBloom(decoder.nextBytesLenPrefix());
  }

  public MetricsPageMetadataSnapshot toSnapshot() {
    return new MetricsPageMetadataSnapshot(this.metricNameTrigrams, this.tagPatternTrigrams);
  }
}
