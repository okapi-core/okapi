package org.okapi.web.service.query;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.okapi.web.auth.AccessManager;
import org.okapi.web.auth.TokenManager;
import org.okapi.web.service.Configs;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class PromQlService {

  private final OkHttpClient client;
  private final TokenManager tokenManager;
  private final AccessManager accessManager;
  private final ProxyResponseTranslator translator;
  private final String clusterEndpoint;

  public PromQlService(
      OkHttpClient client,
      TokenManager tokenManager,
      AccessManager accessManager,
      ProxyResponseTranslator proxyResponseTranslator,
      @Value(Configs.CLUSTER_EP) String clusterEndpoint) {
    this.client = client;
    this.tokenManager = tokenManager;
    this.accessManager = accessManager;
    this.translator = proxyResponseTranslator;
    this.clusterEndpoint = clusterEndpoint;
  }

  public String queryPromQlInstant(String token, String query, String time, String timeout) {
    validateAccess(token);
    var url = new StringBuilder("/api/v1/query?query=").append(encode(query));
    if (time != null) url.append("&time=").append(encode(time));
    if (timeout != null) url.append("&timeout=").append(encode(timeout));
    return getRequest(url.toString());
  }

  public String queryPromQlInstantPost(String token, String query, String time, String timeout) {
    validateAccess(token);
    var form = new java.util.LinkedHashMap<String, String>();
    form.put("query", query);
    if (time != null) form.put("time", time);
    if (timeout != null) form.put("timeout", timeout);
    return postFormRequest("/api/v1/query", form);
  }

  public String queryPromQlRange(
      String token, String query, String start, String end, String step, String timeout) {
    validateAccess(token);
    var url =
        new StringBuilder("/api/v1/query_range?query=")
            .append(encode(query))
            .append("&start=")
            .append(encode(start))
            .append("&end=")
            .append(encode(end))
            .append("&step=")
            .append(encode(step));
    if (timeout != null) url.append("&timeout=").append(encode(timeout));
    return getRequest(url.toString());
  }

  public String queryPromQlLabels(
      String token, String start, String end, List<String> matchers) {
    validateAccess(token);
    var url = new StringBuilder("/api/v1/labels");
    boolean first = true;
    if (start != null) {
      url.append(first ? "?" : "&").append("start=").append(encode(start));
      first = false;
    }
    if (end != null) {
      url.append(first ? "?" : "&").append("end=").append(encode(end));
      first = false;
    }
    if (matchers != null) {
      for (var matcher : matchers) {
        url.append(first ? "?" : "&").append("match[]=").append(encode(matcher));
        first = false;
      }
    }
    return getRequest(url.toString());
  }

  public String queryPromQlLabelValues(
      String token, String label, String start, String end, List<String> matchers) {
    validateAccess(token);
    var url = new StringBuilder("/api/v1/label/").append(encode(label)).append("/values");
    boolean first = true;
    if (start != null) {
      url.append(first ? "?" : "&").append("start=").append(encode(start));
      first = false;
    }
    if (end != null) {
      url.append(first ? "?" : "&").append("end=").append(encode(end));
      first = false;
    }
    if (matchers != null) {
      for (var matcher : matchers) {
        url.append(first ? "?" : "&").append("match[]=").append(encode(matcher));
        first = false;
      }
    }
    return getRequest(url.toString());
  }

  private String postFormRequest(String path, Map<String, String> formFields) {
    var body = new StringBuilder();
    boolean first = true;
    for (var entry : formFields.entrySet()) {
      if (!first) body.append("&");
      body.append(encode(entry.getKey())).append("=").append(encode(entry.getValue()));
      first = false;
    }
    RequestBody requestBody = RequestBody.create(body.toString().getBytes());
    Request request =
        new Request.Builder()
            .url(clusterEndpoint + path)
            .header("Content-Type", "application/x-www-form-urlencoded")
            .post(requestBody)
            .build();
    try (var response = client.newCall(request).execute()) {
      return translator.translateResponse(response, String.class);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private String getRequest(String path) {
    Request request =
        new Request.Builder()
            .url(clusterEndpoint + path)
            .header("Content-Type", "application/json")
            .get()
            .build();
    try (var response = client.newCall(request).execute()) {
      return translator.translateResponse(response, String.class);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static String encode(String input) {
    return URLEncoder.encode(input, StandardCharsets.UTF_8);
  }

  private void validateAccess(String token) {
    var userId = tokenManager.getUserId(token);
    var orgId = tokenManager.getOrgId(token);
    accessManager.checkUserIsOrgMember(new AccessManager.AuthContext(userId, orgId));
  }
}
