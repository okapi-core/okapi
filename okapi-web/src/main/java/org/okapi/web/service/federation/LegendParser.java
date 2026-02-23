/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.web.service.federation;

import org.okapi.web.tsvector.LegendFn;

public interface LegendParser<T> {
  LegendFn<T> createLegend(String def);
}
