package org.okapi.collections;

import java.util.*;

public class OkapiLists {

  public static <T> List<T> toList(Iterator<T> iterator) {
    var array = new ArrayList<T>();
    iterator.forEachRemaining(array::add);
    return array;
  }

  public static <T> List<T> toList(T[] arr) {
    var list = new ArrayList<T>();
    for (int i = 0; i < arr.length; i++) {
      list.add(arr[i]);
    }
    return list;
  }

  public static float[] toFloatArray(List<Float> list) {
    var arr = new float[list.size()];
    for (int i = 0; i < list.size(); i++) {
      arr[i] = list.get(i);
    }
    return arr;
  }

  public static long[] toLongArray(List<Long> list) {
    var arr = new long[list.size()];
    for (int i = 0; i < list.size(); i++) {
      arr[i] = list.get(i);
    }
    return arr;
  }

  public static List<Integer> range(int st, int en) {
    return new ArrayList<>() {
      {
        for (int i = st; i < en; i++) {
          add(i);
        }
      }
    };
  }
}
