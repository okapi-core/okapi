/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.web.investigation.ctx;

import java.io.PrintWriter;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class InstructionCtx implements PrintableContext {
  PrintableContext investigationCtx;
  String instruction;

  @Override
  public void print(PrintWriter writer) {
    investigationCtx.print(writer);
    writer.write("<instruction>");
    writer.write(instruction);
    writer.write("</instruction>");
  }
}
