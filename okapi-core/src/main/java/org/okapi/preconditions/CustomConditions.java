/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.preconditions;

import java.util.function.Supplier;

public class CustomConditions {

  public static <T extends Exception> void checkNotNull(Object object, Supplier<T> ex) throws T {
    if (object == null) {
      throw ex.get();
    }
  }
}
