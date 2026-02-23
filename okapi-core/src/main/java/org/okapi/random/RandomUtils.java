package org.okapi.random;

import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class RandomUtils {

  public <T> T getRandomSample(List<T> collection) {
    var size = collection.size();
    var random = ThreadLocalRandom.current();
    var item = (int) (random.nextDouble() * size);
    return collection.get(item);
  }

  public static <T> T getWeightedRandomSample(
      List<T> collection, List<Double> weights, Random random) {
    if (collection.isEmpty() || weights.size() != collection.size())
      throw new IllegalArgumentException();
    var csum = new double[collection.size() + 1];
    var total = weights.stream().reduce(Double::sum).orElse(0.);
    var weight = random.nextDouble() * total;
    for (int i = 0; i < collection.size(); i++) {
      csum[i + 1] = csum[i] + weights.get(i);
      if (csum[i + 1] > weight) {
        return collection.get(i);
      }
    }
    return collection.getFirst();
  }

  public static byte[] randomBytes(Random random, int size) {
    var bytes = new byte[size];
    random.nextBytes(bytes);
    return bytes;
  }

  public static byte[] randomOtelTraceId(Random random) {
    return randomBytes(random, 16);
  }

  public static byte[] randomOtelTraceId() {
    var random = new Random();
    return randomBytes(random, 16);
  }

  public static byte[] getRanomOtelSpanId(Random random) {
    return randomBytes(random, 8);
  }

  public static byte[] getRanomOtelSpanId() {
    var random = new Random();
    return randomBytes(random, 8);
  }
}
