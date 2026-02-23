package org.okapi.web.service.federation;

import java.util.List;
import org.okapi.web.tsvector.TimeMatrix;
import org.okapi.web.tsvector.TimeVector;

public interface FederatedAgent {
  TimeMatrix getTimeMatrix(AgentContext context, String query) throws Exception;

  TimeVector getTimeVector(AgentContext context, String query) throws Exception;

  List<String> getList(AgentContext context, String query) throws Exception;
}
