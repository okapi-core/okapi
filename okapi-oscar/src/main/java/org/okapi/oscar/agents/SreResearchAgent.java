package org.okapi.oscar.agents;

public interface SreResearchAgent {

  public void respond(String sessionId, long streamId, String userMessage);
}
