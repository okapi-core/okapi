package org.okapi.web.investigation.ctx;

import java.io.PrintWriter;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@AllArgsConstructor
@Builder
@Getter
// todo: remove for an effective agent, simpler prompt = better
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
