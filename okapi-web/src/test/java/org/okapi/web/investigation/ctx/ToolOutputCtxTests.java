/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.web.investigation.ctx;

import java.io.PrintWriter;
import java.io.StringWriter;
import org.junit.jupiter.api.Test;

public class ToolOutputCtxTests {

  @Test
  void testRendering_empty() {
    var toolOutput = new ToolOutputContext();
    var sw = new StringWriter();
    toolOutput.print(new PrintWriter(sw));
  }

  @Test
  void testRendering_withData() {}
}
