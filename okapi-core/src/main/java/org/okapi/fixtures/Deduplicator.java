/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.fixtures;

public class Deduplicator {
  public static <T> String dedup(String input, Class<T> clazz) {
    return clazz.getSimpleName() + "_" + input;
  }

  public static <T> String dedup(String input, String testInstance, Class<T> clazz) {
    return clazz.getSimpleName() + "_" + testInstance.replace("-", "") + input;
  }

  public static String dedup(String dedupStr, String input) {
    return dedupStr + "_" + input;
  }
}
