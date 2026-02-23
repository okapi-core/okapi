/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.web.investigation.ctx;

import java.io.PrintWriter;
import java.util.TreeMap;

public class ToolOutputContext implements PrintableContext {
  TreeMap<Integer, Tooloutput> toolOutputs = new TreeMap<>();

  public void putToolOutput(int order, Tooloutput toolOutput) {
    toolOutputs.put(order, toolOutput);
  }

  @Override
  public void print(PrintWriter writer) {
    writer.write("<tool-outputs>\n");
    for (var stepNumber : toolOutputs.keySet()) {
      var output = toolOutputs.get(stepNumber);
      writer.write("<step-number>");
      writer.write(Integer.toString(stepNumber));
      writer.write("</step-number>");
      writer.write("<output>");
      output.print(writer);
      writer.write("</output>");
    }
    writer.write("</tool-outputs>\n");
  }
}
