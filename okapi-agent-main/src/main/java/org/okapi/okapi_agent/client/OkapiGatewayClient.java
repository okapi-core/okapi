package org.okapi.okapi_agent.client;

import com.google.gson.Gson;
import com.google.inject.Inject;
import java.io.IOException;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.okapi.agent.dto.*;
import org.okapi.okapi_agent.StaticConfig;
import org.okapi.okapi_agent.config.GatewayConfig;
import org.okapi.okapi_agent.jobhandler.HandlerRegistry;
import org.okapi.okapi_agent.messages.ServerMessages;
import org.okapi.web.dtos.pendingjob.GetPendingJobsRequest;

public class OkapiGatewayClient {

  public record UpdateJobResult(boolean jobWasUpdated) {}

  GatewayConfig gatewayConfig;
  OkHttpClient okHttpClient;
  HandlerRegistry handlerRegistry;
  Gson gson = new Gson();
  String authorizationHeader;

  @Inject
  public OkapiGatewayClient(
      GatewayConfig gatewayConfig, OkHttpClient okHttpClient, HandlerRegistry handlerRegistry) {
    this.gatewayConfig = gatewayConfig;
    this.okHttpClient = okHttpClient;
    this.handlerRegistry = handlerRegistry;
    this.authorizationHeader = "Authorization: Bearer " + gatewayConfig.getGatewayToken();
  }

  public void submitResult(String jobId, QueryResult response) {
    var submitResultRequest =
        new Request.Builder()
            .url(gatewayConfig.getEndpoint() + "/api/v1/pending-jobs/" + jobId + "/results")
            .header(RequestHeaders.AUTHORIZATION, "Bearer " + gatewayConfig.getGatewayToken())
            .header(RequestHeaders.OKAPI_AGENT_VERSION, StaticConfig.AGENT_VERSION)
            .post(
                okhttp3.RequestBody.create(
                    gson.toJson(response), okhttp3.MediaType.parse("application/json")))
            .build();
    try (var req = okHttpClient.newCall(submitResultRequest).execute()) {
      if (!req.isSuccessful()) {
        throw new RuntimeException(
            "Failed to submit result: " + req.code() + " - " + req.message());
      }
    } catch (Exception e) {
      throw new RuntimeException("Error while submitting result", e);
    }
  }

  public UpdateJobResult updateJobStatus(String jobId, JOB_STATUS status) {
    var updateRequest = UpdatePendingJobRequest.builder().jobId(jobId).status(status).build();
    var httpRequest =
        new Request.Builder()
            .url(gatewayConfig.getEndpoint() + "/api/v1/pending-jobs/update")
            .header(RequestHeaders.AUTHORIZATION, "Bearer " + gatewayConfig.getGatewayToken())
            .header(RequestHeaders.OKAPI_AGENT_VERSION, StaticConfig.AGENT_VERSION)
            .post(
                okhttp3.RequestBody.create(
                    gson.toJson(updateRequest), okhttp3.MediaType.parse("application/json")))
            .build();
    try (var req = okHttpClient.newCall(httpRequest).execute()) {
      if (!req.isSuccessful()) {
        var code = req.code();
        var msg = req.body().string();
        if (msg.contains(ServerMessages.COULD_NOT_TRANSITION)) {
          return new UpdateJobResult(false);
        } else {
          throw new RuntimeException("Failed to update job status: " + code + " - " + msg);
        }
      }
      return new UpdateJobResult(true);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public ListPendingJobsResponse nextPendingJob() {
    var allSources = handlerRegistry.getRegisteredSourceIds();
    var requestBody = new GetPendingJobsRequest(allSources);
    var pendingJobRequest =
        new Request.Builder()
            .url(gatewayConfig.getEndpoint() + "/api/v1/pending-jobs")
            .header(RequestHeaders.AUTHORIZATION, "Bearer " + gatewayConfig.getGatewayToken())
            .header(RequestHeaders.OKAPI_AGENT_VERSION, StaticConfig.AGENT_VERSION)
            .header("Content-Type", "application/json")
            .post(RequestBody.create(gson.toJson(requestBody).getBytes()))
            .build();
    try (var req = okHttpClient.newCall(pendingJobRequest).execute()) {
      if (!req.isSuccessful()) {
        throw new RuntimeException(
            "Failed to fetch next pending job: " + req.code() + " - " + req.message());
      }
      var body = req.body();
      var bodyStr = body.string();
      if (bodyStr.isBlank()) {
        throw new RuntimeException("Failed to fetch next pending job: empty response body");
      }
      return gson.fromJson(bodyStr, ListPendingJobsResponse.class);
    } catch (Exception e) {
      throw new RuntimeException("Error while fetching next pending job", e);
    }
  }

  public void submitError(String jobId, QueryResult response) {
    var errorRequest =
        new Request.Builder()
            .url(gatewayConfig.getEndpoint() + "/api/v1/pending-jobs/" + jobId + "/errors")
            .header(RequestHeaders.AUTHORIZATION, "Bearer " + gatewayConfig.getGatewayToken())
            .header(RequestHeaders.OKAPI_AGENT_VERSION, StaticConfig.AGENT_VERSION)
            .post(
                okhttp3.RequestBody.create(
                    gson.toJson(response), okhttp3.MediaType.parse("application/json")))
            .build();
    try (var req = okHttpClient.newCall(errorRequest).execute()) {
      if (!req.isSuccessful()) {
        throw new RuntimeException("Failed to submit error: " + req.code());
      }
    } catch (Exception e) {
      throw new RuntimeException("Error while submitting error", e);
    }
  }
}
