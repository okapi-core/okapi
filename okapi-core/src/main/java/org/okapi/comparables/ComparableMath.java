/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.comparables;

public class ComparableMath {
  public static <T extends Comparable<T>> T max(T left, T right) {
    var cmp = left.compareTo(right);
    return (cmp <= 0) ? left : right;
  }

  public static <T extends Comparable<T>> T min(T left, T right) {
    var cmp = left.compareTo(right);
    return (cmp <= 0) ? right : left;
  }
}
