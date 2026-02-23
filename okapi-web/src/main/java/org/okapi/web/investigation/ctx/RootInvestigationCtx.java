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
public class RootInvestigationCtx implements PrintableContext {
  ToplineInstruction toplineInstruction;
  InvestigationTopicCtx investigationTopicCtx;
  ToolWikiCtx toolWikiCtx;
  ToolOutputContext outputContext;
  DependencyContext dependencyContext;

  @Override
  public void print(PrintWriter writer) {
    toplineInstruction.print(writer);
    investigationTopicCtx.print(writer);
    toolWikiCtx.print(writer);
    outputContext.print(writer);
    dependencyContext.print(writer);
  }

  public void consumeOutput(Integer stepNumber, Tooloutput tooloutput) {
    outputContext.putToolOutput(stepNumber, tooloutput);
  }
}
