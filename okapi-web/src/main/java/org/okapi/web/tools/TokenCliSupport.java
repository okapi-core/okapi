/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.web.tools;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.okapi.web.dtos.auth.CreateUserRequest;
import org.okapi.web.dtos.auth.SignInRequest;
import org.okapi.web.dtos.auth.TokenResponse;
import org.okapi.web.dtos.org.ListOrgsResponse;
import org.okapi.web.dtos.token.ApiToken;
import org.okapi.web.dtos.users.GetOrgUserView;
import org.okapi.web.headers.RequestHeaders;

class TokenCliSupport {
  private static final MediaType JSON = MediaType.get("application/json");

  private final OkHttpClient httpClient;
  private final Gson gson;
  private final String endpoint;

  TokenCliSupport(String endpoint) {
    this(endpoint, new OkHttpClient(), new Gson());
  }

  TokenCliSupport(String endpoint, OkHttpClient httpClient, Gson gson) {
    this.endpoint = endpoint;
    this.httpClient = httpClient;
    this.gson = gson;
  }

  TempTokenContext fetchTempToken(String email, String password) throws IOException {
    signUpIgnoreIfExists(email, password);
    var loginToken = signIn(email, password);
    var orgId = getFirstOrgId(loginToken);
    var tempToken = createTempToken(loginToken, orgId);
    return new TempTokenContext(loginToken, orgId, tempToken);
  }

  ApiToken createApiToken(String tempToken) throws IOException {
    var headers = Headers.of(RequestHeaders.TEMP_TOKEN, tempToken);
    return postJson("/api/v1/tokens", "{}", headers, ApiToken.class);
  }

  private void signUpIgnoreIfExists(String email, String password) {
    var signUpRequest =
        CreateUserRequest.builder().email(email).password(password).firstName("Test").build();
    try {
      postJson("/api/v1/users", signUpRequest, null, TokenResponse.class);
    } catch (IOException e) {
      System.err.println("Signup failed (continuing): " + e.getMessage());
    }
  }

  private String signIn(String email, String password) throws IOException {
    var signInRequest = SignInRequest.builder().email(email).password(password).build();
    var response = postJson("/api/v1/users/signin", signInRequest, null, TokenResponse.class);
    return Objects.requireNonNull(response, "Empty sign-in response").getToken();
  }

  private String getFirstOrgId(String loginToken) throws IOException {
    var headers = Headers.of(RequestHeaders.LOGIN_TOKEN, loginToken);
    var orgsResponse = getJson("/api/v1/orgs", headers, ListOrgsResponse.class);
    List<GetOrgUserView> orgs =
        orgsResponse == null
            ? List.of()
            : Objects.requireNonNullElse(orgsResponse.getOrgs(), List.of());
    if (orgs.isEmpty()) {
      throw new IOException("No orgs found for the user.");
    }
    return orgs.get(0).getOrgId();
  }

  private String createTempToken(String loginToken, String orgId) throws IOException {
    var headers = Headers.of(RequestHeaders.LOGIN_TOKEN, loginToken);
    var response =
        postJson("/api/v1/auth/" + orgId + "/session", "{}", headers, TokenResponse.class);
    return Objects.requireNonNull(response, "Empty temp token response").getToken();
  }

  private <T> T postJson(String path, Object payload, Headers headers, Class<T> clazz)
      throws IOException {
    String jsonPayload;
    if (payload == null) {
      jsonPayload = "{}";
    } else if (payload instanceof String asString) {
      jsonPayload = asString;
    } else {
      jsonPayload = gson.toJson(payload);
    }

    RequestBody requestBody = RequestBody.create(jsonPayload, JSON);
    Request.Builder builder = new Request.Builder().url(endpoint + path).post(requestBody);
    if (headers != null) {
      builder.headers(headers);
    }
    return execute(builder.build(), clazz, path);
  }

  private <T> T getJson(String path, Headers headers, Class<T> clazz) throws IOException {
    Request.Builder builder = new Request.Builder().url(endpoint + path).get();
    if (headers != null) {
      builder.headers(headers);
    }
    return execute(builder.build(), clazz, path);
  }

  private <T> T execute(Request request, Class<T> clazz, String path) throws IOException {
    try (Response response = httpClient.newCall(request).execute()) {
      String responseBody = response.body() != null ? response.body().string() : "";
      if (!response.isSuccessful()) {
        throw new IOException(
            "Request failed for " + path + " (status " + response.code() + "): " + responseBody);
      }
      if (clazz == null || clazz == Void.class) {
        return null;
      }
      try {
        return gson.fromJson(responseBody, clazz);
      } catch (JsonSyntaxException e) {
        throw new IOException("Failed to parse response for " + path + ": " + responseBody, e);
      }
    }
  }

  static class TempTokenContext {
    private final String loginToken;
    private final String orgId;
    private final String tempToken;

    TempTokenContext(String loginToken, String orgId, String tempToken) {
      this.loginToken = loginToken;
      this.orgId = orgId;
      this.tempToken = tempToken;
    }

    String loginToken() {
      return loginToken;
    }

    String orgId() {
      return orgId;
    }

    String tempToken() {
      return tempToken;
    }
  }
}
