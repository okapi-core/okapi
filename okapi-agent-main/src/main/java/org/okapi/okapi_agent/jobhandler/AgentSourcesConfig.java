package org.okapi.okapi_agent.jobhandler;

import lombok.Data;
import lombok.Getter;

import java.util.List;

@Data
public class AgentSourcesConfig {
    private List<SourceDefinition> sources;
}

