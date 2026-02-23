/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.web.investigation.ctx;

import java.io.PrintWriter;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@AllArgsConstructor
@Builder
@Getter
public class InvestigationTopicCtx implements PrintableContext {
  String investigationId;
  String investigationTopic;

  @Override
  public void print(PrintWriter writer) {
    writer.write("<investigation-topic>" + investigationTopic + "</investigation-topic>\n");
  }
}
