/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.web.investigation.ctx;

import java.io.PrintWriter;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@AllArgsConstructor
@Builder
@Getter
public class StoreContext implements PrintableContext {
  // todo: unit-test
  String metricsStore;
  String tracesStore;
  String logsStore;

  @Override
  public void print(PrintWriter writer) {
    writer.write("<logs-store>");
    writer.write(logsStore);
    writer.write("</logs-store>");
    writer.write("<metrics-store>");
    writer.write(metricsStore);
    writer.write("</metrics-store>");
    writer.write("<traces-store>");
    writer.write(tracesStore);
    writer.write("</traces-store>");
  }
}
