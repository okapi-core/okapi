package org.okapi.testutils;

import java.util.*;
import org.apache.commons.lang3.RandomStringUtils;
import org.okapi.Statistics;
import org.okapi.metrics.rollup.RollupSeries;
import org.okapi.metrics.rollup.TsReader;
import org.okapi.metrics.stats.KllSketchRestorer;
import org.okapi.metrics.stats.RolledUpStatistics;
import org.okapi.metrics.stats.UpdatableStatistics;

public class OkapiTestUtils {

  public static List<Float> genRandom(float base, float scale, int n) {
    var l = new ArrayList<Float>();
    for (int i = 0; i < n; i++) {
      l.add(genSingle(base, scale));
    }
    return l;
  }

  public static String smallId(int n) {
    return RandomStringUtils.insecure().next(n, true, false);
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
    assert A.getCount() == B.getCount()
        : "Expected equal counts but got " + A.getCount() + " and " + B.getCount();
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

  public <T> String dedup(Class<T> clazz, String id) {
    return clazz.getSimpleName() + "AND" + id;
  }

  public static boolean checkMatchesReferenceFuzzy(
      RollupSeries<UpdatableStatistics> ref, TsReader tsReader) throws InterruptedException {
    for (var key : ref.getKeys()) {
      var statsA =
          RolledUpStatistics.deserialize(ref.getSerializedStats(key), new KllSketchRestorer());
      var statsB = tsReader.getStat(key);
      var waitTime = 10_000 + System.currentTimeMillis();
      while (statsB.isEmpty() && System.currentTimeMillis() < waitTime) {
        Thread.sleep(1_000);
        statsB = tsReader.getStat(key);
      }
      assert statsB.isPresent() : "Could not find a value for key: " + key + " in the reader.";
      assertStatsEquals(statsA, statsB.get());
    }
    return true;
  }

  public static boolean checkEquals(
      RollupSeries<UpdatableStatistics> series1, RollupSeries<UpdatableStatistics> series2) {
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
