package org.okapi.web.investigation.ctx;

import java.io.PrintWriter;
import java.util.List;

public class TagDescription implements PrintableContext {
  public record Attribute(String name, String description) {}

  String tag;
  List<Attribute> attributes;

  @Override
  public void print(PrintWriter writer) {
    writer.write(
        "Below we provide a list of tags. Tags may have attributes that provide additional context or details about the tag.\n");
    writer.write(
        "Tags follow XML-like syntax, where attributes are specified within the opening tag.\n\n");
    writer.write("Tag: " + tag + "\n");
    for (var attr : attributes) {
      writer.write(" - " + attr.name + ": " + attr.description + "\n");
    }
  }
}
