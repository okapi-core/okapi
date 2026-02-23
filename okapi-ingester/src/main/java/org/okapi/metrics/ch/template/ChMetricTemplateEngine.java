/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.metrics.ch.template;

import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import gg.jte.TemplateException;
import gg.jte.TemplateOutput;
import org.springframework.stereotype.Component;

@Component
public class ChMetricTemplateEngine {

  private final TemplateEngine engine;

  public ChMetricTemplateEngine() {
    engine = TemplateEngine.createPrecompiled(ContentType.Plain);
  }

  public void render(String name, Object param, TemplateOutput output) throws TemplateException {
    engine.render(name, param, output);
  }
}
