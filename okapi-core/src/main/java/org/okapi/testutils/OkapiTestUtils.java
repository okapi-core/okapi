package org.okapi.testutils;

import com.google.common.base.Preconditions;
import org.okapi.metrics.rollup.RollupSeries;
import org.okapi.metrics.stats.KllSketchRestorer;
import org.okapi.metrics.stats.Statistics;
import java.util.*;

public class OkapiTestUtils {

  public static List<Float> genRandom(float base, float scale, int n) {
    var l = new ArrayList<Float>();
    for (int i = 0; i < n; i++) {
      l.add(genSingle(base, scale));
    }
    return l;
  }

  public static float genSingle(float base, float scale) {
    var r = new Random();
    return base + scale * r.nextFloat();
  }

  public static List<Long> getTimes(long start, int back, int fwd, int n) {
    var L = new ArrayList<Long>();
    L.add(start);
    var time = start;
    for (int i = 1; i < n; i++) {
      time = genTs(time, back, fwd);
      L.add(time);
    }
    return L;
  }

  public static byte[] genBytes(int n) {
    var bytes = new byte[n];
    var random = new Random();
    random.nextBytes(bytes);
    return bytes;
  }

  public static long genTs(long start, int back, int fwd) {
    var r = new Random();
    var delta = r.nextInt(back, fwd);
    return start + delta;
  }

  public static <T> List<T> copyAndAppend(List<T> list, T val) {
    var arrayList = new ArrayList<>(list);
    arrayList.add(val);
    return arrayList;
  }

  public static List<Long> toList(long[] arr) {
    return new ArrayList<>() {
      {
        for (int i = 0; i < arr.length; i++) add(arr[i]);
      }
    };
  }

  public static List<Float> toList(float[] arr) {
    return new ArrayList<>() {
      {
        for (int i = 0; i < arr.length; i++) add(arr[i]);
      }
    };
  }

  public static float getPercentile(List<Float> ref, double percentile) {
    var copy = new ArrayList<>(ref);
    Collections.sort(copy);
    var idx = percentile * copy.size();
    var lower = (int) idx;
    if (lower == ref.size()) {
      return copy.get(lower - 1);
    }
    if (lower == idx) {
      return copy.get(lower);
    }
    var upper = 1 + lower;
    var fractional = (float) (idx - lower);
    if (upper < copy.size()) {
      return (1 - fractional) * copy.get(lower) + fractional * copy.get(upper);
    } else {
      return copy.get(lower);
    }
  }

  public static boolean bytesAreEqual(byte[] a, byte[] b) {
    if (a.length != b.length) {
      return false;
    }
    for (int i = 0; i < a.length; i++) {
      if (a[i] != b[i]) {
        return false;
      }
    }
    return true;
  }

  public static void assertStatsEquals(Statistics A, Statistics B) {
    assert A.getCount() == B.getCount();
    assert A.getSum() == B.getSum();
    assert A.avg() == B.avg();
    assert A.min() == B.min();
    var percentiles = Arrays.asList(0.0f, 0.25f, 0.50f, 0.75f, 0.9f, 0.95f, 0.99f, 0.999f);
    for (var p : percentiles) {
      var pA = A.percentile(p);
      var pB = B.percentile(p);
      var error = Math.abs(pA - pB) / (1e-6f + pA);
      assert error < 0.01
          : String.format(
              "error should be less than 1percent but was %f . pA = %f, pB = %f", error, pA, pB);
    }
  }

  public static boolean checkMatchesReferenceFuzzy(RollupSeries ref, RollupSeries series2) {
    for (var key : series2.getKeys()) {
      var statsA = Statistics.deserialize(ref.getSerializedStats(key), new KllSketchRestorer());
      var statsB = Statistics.deserialize(series2.getSerializedStats(key), new KllSketchRestorer());
      assertStatsEquals(statsA, statsB);
    }
    return true;
  }

  public static void checkMatchesReference(RollupSeries ref, RollupSeries target) {
    Preconditions.checkNotNull(ref, "Reference is null.");
    for (var k : target.getKeys()) {
      var fromRef = ref.getSerializedStats(k);
      assert OkapiTestUtils.bytesAreEqual(fromRef, target.getSerializedStats(k))
          : "Bytes don't match";
    }
  }

  public static boolean checkEquals(RollupSeries series1, RollupSeries series2) {
    if (series1.getKeys().size() != series2.getKeys().size()) {
      return false;
    }
    assert series1.getKeys().equals(series2.getKeys()) : "Keys should match";
    for (var key : series1.getKeys()) {
      assert OkapiTestUtils.bytesAreEqual(
              series1.getSerializedStats(key), series2.getSerializedStats(key))
          : "Serialized stats for key " + key + " do not match";
    }
    return true;
  }
}
