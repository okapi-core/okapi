package org.okapi.metrics.stats;

import com.google.common.primitives.Ints;
import java.io.ByteArrayOutputStream;
import lombok.Getter;

@Getter
public class HistoStats {
  float[] buckets;
  int[] bucketCounts;

  public HistoStats(float[] buckets, int[] bucketCounts) {
    if (buckets.length + 1 != bucketCounts.length) {
      throw new IllegalArgumentException(
          String.format(
              "Malformed histogram, length of bucket and bucket counts are %d and %d",
              buckets.length, bucketCounts.length));
    }
    this.buckets = buckets;
    this.bucketCounts = bucketCounts;
  }

  public byte[] serialize() {
    var n = buckets.length;
    var byteStream = new ByteArrayOutputStream();
    byteStream.writeBytes(Ints.toByteArray(n));
    for (var bucket : buckets) {
      var val = Ints.toByteArray(Float.floatToIntBits(bucket));
      byteStream.writeBytes(val);
    }
    for (var count : bucketCounts) {
      var val = Ints.toByteArray(count);
      byteStream.writeBytes(val);
    }
    return byteStream.toByteArray();
  }

  public static HistoStats deserialize(byte[] bytes) {
    if (bytes.length < 4) {
      throw new IllegalArgumentException("Expected atleast 4 bytes in the array.");
    }
    var n = Ints.fromBytes(bytes[0], bytes[1], bytes[2], bytes[3]);
    var buckets = new float[n];
    // 4 bytes for the length, 4n for the buckets, 4 * (1 + n) for the counts.
    var expectedSize = (4) + (4 * n) + 4 * (1 + n);
    if (bytes.length != expectedSize) {
      throw new IllegalArgumentException(
          String.format(
              "Not enough bytes to deserialize, expected %d but got %d",
              expectedSize, bytes.length));
    }
    var off = 4;
    for (int i = 0; i < n; i++) {
      var bytesOffset = i * 4;
      buckets[i] =
          Float.intBitsToFloat(
              Ints.fromBytes(
                  bytes[off + bytesOffset],
                  bytes[off + bytesOffset + 1],
                  bytes[off + bytesOffset + 2],
                  bytes[off + bytesOffset + 3]));
    }
    off = 4 + n * 4;
    var bucketCounts = new int[n + 1];
    for (int i = 0; i <= n; i++) {
      var bytesOffset = i * 4;
      bucketCounts[i] =
          Ints.fromBytes(
              bytes[off + bytesOffset],
              bytes[off + bytesOffset + 1],
              bytes[off + bytesOffset + 2],
              bytes[off + bytesOffset + 3]);
    }
    return new HistoStats(buckets, bucketCounts);
  }
}
