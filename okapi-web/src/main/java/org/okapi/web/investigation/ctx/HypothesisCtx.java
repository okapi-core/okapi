/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.web.investigation.ctx;

import java.io.PrintWriter;
import lombok.AllArgsConstructor;
import lombok.Builder;

@AllArgsConstructor
@Builder
public class HypothesisCtx implements PrintableContext {
  RootInvestigationCtx investigationCtx;
  String claim;

  @Override
  public void print(PrintWriter writer) {
    investigationCtx.print(writer);
    writer.write("<hypothesis>");
    writer.write(claim);
    writer.write("</hypothesis>");
  }
}
