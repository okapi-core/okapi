package org.okapi.okapi_agent.jobhandler;

import java.util.Map;

// todo: pick up from here -> add a source definition -> add a parser for the definition file ->
// integrate with job handler
public record SourceDefinition(String id, Map<String, Object> config) {}
