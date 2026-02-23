package org.okapi.web.investigation.ctx;

import java.io.PrintWriter;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class ToolWikiCtx implements PrintableContext {

  @Override
  public void print(PrintWriter writer) {
    writer.write("<tools>\n");
    writer.write("</tools>\n");
  }
}
