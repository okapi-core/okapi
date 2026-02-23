/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.traces.ch.template;

import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import gg.jte.TemplateException;
import gg.jte.TemplateOutput;
import gg.jte.output.StringOutput;
import org.springframework.stereotype.Component;

@Component
public class ChTraceTemplateEngine {

  private final TemplateEngine engine;

  public ChTraceTemplateEngine() {
    engine = TemplateEngine.createPrecompiled(ContentType.Plain);
  }

  public void render(String name, Object param, TemplateOutput output) throws TemplateException {
    engine.render(name, param, output);
  }

  public String render(String name, Object data) {
    var output = new StringOutput();
    render(name, data, output);
    return output.toString();
  }
}
