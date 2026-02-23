package org.okapi.okapi_agent.connection;

import java.util.Map;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.okapi.agent.dto.*;

@AllArgsConstructor
public class HttpConnection {

  HttpConnectionDetails connectionDetails;
  OkHttpClient httpClient;

  public static HttpConnection of(OkHttpClient httpClient, Map<String, Object> cfg) {
    return switch (cfg.get("headers")) {
      case Map<?, ?> headersMap -> build(httpClient, headersMap);
      default -> throw new IllegalArgumentException("Invalid headers configuration");
    };
  }

  private static HttpConnection build(OkHttpClient httpClient, Map<?, ?> headersMap) {
    var headers =
        headersMap.entrySet().stream()
            .filter(e -> e.getKey() instanceof String k && e.getValue() instanceof String v)
            .collect(Collectors.toMap(e -> (String) e.getKey(), e -> (String) e.getValue()));

    return new HttpConnection(new HttpConnectionDetails(headers), httpClient);
  }

  public QueryResult sendRequest(String host, AgentQueryRecords.HttpQuery httpQuery) {
    // Placeholder implementation
    var request =
        (switch (httpQuery) {
              case AgentQueryRecords.HttpQuery(
                      HTTP_METHOD method,
                      String path,
                      java.util.Map<String, String> requestHeaders,
                      String requestBody)
                  when method == HTTP_METHOD.GET ->
                  new Request.Builder().get();
              case AgentQueryRecords.HttpQuery(
                      HTTP_METHOD method,
                      String path,
                      java.util.Map<String, String> requestHeaders,
                      String requestBody)
                  when method == HTTP_METHOD.POST ->
                  new Request.Builder().post(RequestBody.create(requestBody.getBytes()));
              case AgentQueryRecords.HttpQuery(
                      HTTP_METHOD method,
                      String path,
                      java.util.Map<String, String> requestHeaders,
                      String requestBody)
                  when method == HTTP_METHOD.PUT ->
                  throw new IllegalArgumentException("PUT method not implemented yet");
              case AgentQueryRecords.HttpQuery(
                      HTTP_METHOD method,
                      String path,
                      java.util.Map<String, String> requestHeaders,
                      String requestBody)
                  when method == HTTP_METHOD.DELETE ->
                  throw new IllegalArgumentException("DELETE method not implemented yet");
              default -> throw new IllegalArgumentException("Unsupported HTTP method");
            })
            .url(mergeHostAndTarget(host, httpQuery.path()))
            .headers(merged(httpQuery.requestHeaders()))
            .build();
    try (var res = httpClient.newCall(request).execute()) {
      if (res.isSuccessful()) {
        var body = res.body();
        return QueryResult.ofData(body.string());
      } else {
        var maybeBody = res.body();
        var err =
            "HTTP request failed with status code "
                + res.code()
                + " and error: "
                + maybeBody.string();
        return QueryResult.ofError(err);
      }
    } catch (Exception e) {
      var message = e.getMessage() == null ? "unknown error" : e.getMessage();
      return QueryResult.ofError("HTTP request failed: " + message);
    }
  }

  public String mergeHostAndTarget(String host, String target) {
    if (host.endsWith("/") && target.startsWith("/")) {
      return host + target.substring(1);
    } else if (!host.endsWith("/") && !target.startsWith("/")) {
      return host + "/" + target;
    } else {
      return host + target;
    }
  }

  public Headers merged(Map<String, String> requestHeaders) {
    var combined = new java.util.HashMap<>(connectionDetails.headers());
    combined.putAll(requestHeaders);
    return Headers.of(combined);
  }
}
