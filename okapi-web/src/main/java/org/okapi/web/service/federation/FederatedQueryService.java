package org.okapi.web.service.federation;

import org.okapi.exceptions.BadRequestException;
import org.okapi.parallel.ParallelExecutor;
import org.okapi.web.auth.AccessManager;
import org.okapi.web.auth.TokenManager;
import org.okapi.web.dtos.federation.FederatedQueryRequest;
import org.okapi.web.dtos.federation.FederatedQueryResponse;
import org.okapi.web.service.QueryHydrator;
import org.okapi.web.service.federation.converter.FederatedDataTypeFactory;
import org.okapi.web.service.federation.locator.AgentLocator;
import org.springframework.stereotype.Service;

@Service
public class FederatedQueryService {

  AccessManager accessManager;
  TokenManager tokenManager;
  QueryHydrator hydrator;
  AgentLocator agentLocator;
  FederatedDataTypeFactory dataTypeFactory;
  ParallelExecutor parallelExecutor;

  public FederatedQueryService(
      AccessManager accessManager,
      TokenManager tokenManager,
      QueryHydrator hydrator,
      AgentLocator agentLocator,
      FederatedDataTypeFactory dataTypeFactory) {
    this.accessManager = accessManager;
    this.tokenManager = tokenManager;
    this.hydrator = hydrator;
    this.agentLocator = agentLocator;
    this.dataTypeFactory = dataTypeFactory;
  }

  public FederatedQueryResponse query(String tempToken, FederatedQueryRequest queryRequest)
      throws Exception {
    var orgId = tokenManager.getOrgId(tempToken);
    var userId = tokenManager.getUserId(tempToken);
    accessManager.checkUserHasIsOrgMember(userId, orgId);
    // template sub
    var hydrated = hydrator.hydrate(queryRequest.getQuery(), queryRequest.getQueryContext());
    // query dispatch to the right agent
    var agent = agentLocator.getAgent(orgId, queryRequest.getSourceId());
    // query
    var federatedQueryResponse =
        FederatedQueryResponse.builder()
            .sourceId(queryRequest.getSourceId())
            .resultType(queryRequest.getExpected())
            .queryContext(queryRequest.getQueryContext())
            .queryExecuted(hydrated)
            .legendFn(queryRequest.getLegendFn())
            .build();

    switch (queryRequest.getExpected()) {
      case STR_LIST -> {
        var response = agent.getList(AgentContext.makeContext(queryRequest), hydrated);
        var converted = dataTypeFactory.createStringList(response);
        federatedQueryResponse.setStringList(converted);
        return federatedQueryResponse;
      }
      case TIME_VECTOR -> {
        var response = agent.getTimeVector(AgentContext.makeContext(queryRequest), hydrated);
        var converted = dataTypeFactory.createTimeVector(response);
        federatedQueryResponse.setTimeVector(converted);
        return federatedQueryResponse;
      }
      case TIME_MATRIX -> {
        var response = agent.getTimeMatrix(AgentContext.makeContext(queryRequest), hydrated);
        var converted = dataTypeFactory.createTimeMatrix(response);
        federatedQueryResponse.setTimeMatrix(converted);
        return federatedQueryResponse;
      }
      default ->
          throw new BadRequestException(
              "Unsupported expected result type: "
                  + queryRequest.getExpected()
                  + " . Query is likely malformed.");
    }
  }
}
