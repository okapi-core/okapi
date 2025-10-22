package org.okapi.logs;

import java.util.List;
import java.util.Random;

public class RandomSampler {

  public static  <E> E randomSample(List<E> elems) {
    var size = elems.size();
    var rng = new Random();
    var idx = rng.nextInt(0, size);
    return elems.get(idx);
  }
}
