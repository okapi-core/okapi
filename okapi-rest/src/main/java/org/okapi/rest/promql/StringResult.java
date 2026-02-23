/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.rest.promql;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Data;

/** Matches STRING result – same structure as ScalarResult. */
@Data
@AllArgsConstructor
public class StringResult {
  @SerializedName(value = "0")
  private double timestamp;

  @SerializedName(value = "1")
  private String value;
}
