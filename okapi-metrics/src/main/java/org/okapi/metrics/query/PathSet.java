/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.metrics.query;

import java.util.List;
import java.util.Map;

public interface PathSet {
  public Map<String, List<String>> list();
}
