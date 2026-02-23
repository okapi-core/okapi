package org.okapi.okapi_agent.jobhandler;


import org.okapi.agent.dto.PendingJob;
import org.okapi.agent.dto.QueryResult;

public interface JobHandler {
    QueryResult getResults(PendingJob pendingJob);
    String getSourceId();
}
