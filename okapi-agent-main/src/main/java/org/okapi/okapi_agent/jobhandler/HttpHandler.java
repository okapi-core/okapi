/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.okapi_agent.jobhandler;

import com.google.common.annotations.VisibleForTesting;
import org.okapi.agent.dto.AgentQueryRecords;
import org.okapi.agent.dto.PendingJob;
import org.okapi.agent.dto.QueryResult;
import org.okapi.okapi_agent.connection.HttpConnection;
import org.okapi.okapi_agent.query.HttpQueryDeserializer;

public class HttpHandler implements JobHandler {
  public record SourceIdAndHost(String sourceId, String host) {}

  HttpQueryDeserializer deserializer;
  HttpConnection connection;
  SourceIdAndHost sourceAndHost;

  public HttpHandler(
      SourceIdAndHost idAndHost, HttpQueryDeserializer deserializer, HttpConnection connection) {
    this.sourceAndHost = idAndHost;
    this.deserializer = deserializer;
    this.connection = connection;
  }

  @VisibleForTesting
  protected void setHost(String host) {
    this.sourceAndHost = new SourceIdAndHost(this.sourceAndHost.sourceId(), host);
  }

  @Override
  public QueryResult getResults(PendingJob pendingJob) {
    if (!pendingJob.getSourceId().equals(sourceAndHost.sourceId())) {
      return QueryResult.builder()
          .error(
              String.format(
                  "Query dispatched to wrong client. Configured: %s but got %s",
                  sourceAndHost.sourceId(), pendingJob.getSourceId()))
          .build();
    }
    var deserialized = deserializer.readQuery(pendingJob.getSpec());
    switch (deserialized) {
      case HttpQueryDeserializer.DeserializationResult(
          AgentQueryRecords.HttpQuery query,
          boolean success,
          String error)
      when !success:
        return QueryResult.builder().processingError(error).build();
      case HttpQueryDeserializer.DeserializationResult(
          AgentQueryRecords.HttpQuery query,
          boolean success,
          String error):
        return connection.sendRequest(sourceAndHost.host(), query);
      default:
        throw new IllegalStateException(
            "Deserialized query is not of type QueryResponse: " + deserialized.getClass());
    }
  }

  @Override
  public String getSourceId() {
    return sourceAndHost.sourceId();
  }
}
