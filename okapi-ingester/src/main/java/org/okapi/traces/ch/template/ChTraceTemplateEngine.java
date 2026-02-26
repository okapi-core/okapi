/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.traces.ch.template;

import gg.jte.TemplateException;
import gg.jte.TemplateOutput;
import org.okapi.metrics.ch.template.ChTemplateEngine;
import org.springframework.stereotype.Component;

@Component
public class ChTraceTemplateEngine extends ChTemplateEngine {

  private final ChTemplateEngine engine;

  public ChTraceTemplateEngine() {
    engine = new ChTemplateEngine();
  }

  public void render(String name, Object param, TemplateOutput output) throws TemplateException {
    engine.render(name, param, output);
  }

  public String render(String name, Object data) {
    return engine.render(name, data);
  }
}
