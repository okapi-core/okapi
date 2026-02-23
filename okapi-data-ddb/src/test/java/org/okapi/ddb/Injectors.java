/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.ddb;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.okapi.data.DaoModule;

public class Injectors {

  public static Injector createTestInjector() {
    return Guice.createInjector(new DaoModule(), new TestDepsModule());
  }
}
