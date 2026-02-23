package org.okapi.web.investigation.ctx;

import java.io.PrintWriter;
import org.okapi.resourcereader.ClasspathResourceReader;

public class ToplineInstruction implements PrintableContext {
  String promptPath = "investigation/topline-instruction.md";
  String instruction;

  public ToplineInstruction() {
    this.instruction = ClasspathResourceReader.readResource(promptPath);
  }

  @Override
  public void print(PrintWriter writer) {
    writer.write(instruction);
  }
}
