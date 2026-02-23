/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.web.investigation.ctx;

import java.io.PrintWriter;
import java.util.Map;

public class TagDescriptionContext implements PrintableContext {
  Map<String, TagDescription> tags;

  @Override
  public void print(PrintWriter writer) {
    writer.write("<tags>");
    for (TagDescription tagDescription : tags.values()) {
      tagDescription.print(writer);
    }
    writer.write("\n");
    writer.write("</tags>");
  }
}
