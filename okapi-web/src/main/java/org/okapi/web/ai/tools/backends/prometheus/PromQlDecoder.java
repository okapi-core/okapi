/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.web.ai.tools.backends.prometheus;

import org.apache.commons.lang3.NotImplementedException;
import org.okapi.web.ai.tools.results.MetricsDecodingResult;

public class PromQlDecoder {

  public static MetricsDecodingResult mapToGaugeSignal(String json) {
    throw new NotImplementedException();
  }

  public static MetricsDecodingResult mapToHistoSignal(String json) {
    throw new NotImplementedException();
  }
}
