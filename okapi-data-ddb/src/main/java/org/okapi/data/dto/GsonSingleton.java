/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.data.dto;

import com.google.gson.Gson;

public class GsonSingleton {
  public static final Gson SINGLETON = new Gson();
}
