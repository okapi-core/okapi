/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.web.investigation.agents;

import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.agent.tool.ToolSpecifications;
import java.util.List;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.okapi.web.ai.tools.impl.GetLogsTool;
import org.okapi.web.ai.tools.impl.GetMetricsTool;
import org.okapi.web.ai.tools.impl.GetTracesTool;

@AllArgsConstructor
@Getter
@Builder
public class InvestigationToolkit {
  GetMetricsTool getMetricsTool;
  GetTracesTool getTracesTool;
  GetLogsTool getLogsTool;

  public List<ToolSpecification> getTools() {
    var metricsTools = ToolSpecifications.toolSpecificationsFrom(GetMetricsTool.class);
    var tracesTools = ToolSpecifications.toolSpecificationsFrom(GetTracesTool.class);
    var logsTools = ToolSpecifications.toolSpecificationsFrom(GetLogsTool.class);
    return Stream.of(metricsTools, tracesTools, logsTools).toList().stream()
        .flatMap(List::stream)
        .toList();
  }
}
