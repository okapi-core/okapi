package org.okapi.staticserver;

import com.google.gson.Gson;
import com.okapi.rest.auth.AuthRequest;
import com.okapi.rest.auth.TokenResponse;
import org.okapi.headers.CookiesAndHeaders;
import org.okapi.staticserver.exceptions.BadRequestException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AuthenticationServer {
  @Autowired ApiProxy apiProxy;
  @Autowired Endpoints endpoints;

  public ApiResponse<TokenResponse> signUp(AuthRequest authRequest)
      throws IOException, BadRequestException {
    var endpoint = endpoints.getSignupEndpoint();
    var gson = new Gson();
    var request =
        new Request.Builder()
            .url(endpoint)
            .post(RequestBody.create(gson.toJson(authRequest).getBytes(StandardCharsets.UTF_8)))
            .header("Content-Type", "application/json")
            .build();
    return apiProxy.handleRequest(request, TokenResponse.class);
  }

  public ApiResponse<TokenResponse> signIn(AuthRequest authRequest)
      throws IOException, BadRequestException {
    var endpoint = endpoints.getSigninEndpoint();
    var gson = new Gson();
    var request =
        new Request.Builder()
            .url(endpoint)
            .post(RequestBody.create(gson.toJson(authRequest).getBytes(StandardCharsets.UTF_8)))
            .header("Content-Type", "application/json")
            .build();
    return apiProxy.handleRequest(request, TokenResponse.class);
  }

  public ApiResponse<TokenResponse> tempToken(String loginToken)
      throws IOException, BadRequestException {
    var endpoint = endpoints.getTempTokenEndpoint();
    var request =
        new Request.Builder()
            .post(RequestBody.create(new byte[0]))
            .url(endpoint)
            .header("Content-Type", "application/json")
            .header(CookiesAndHeaders.HEADER_LOGIN_TOKEN, loginToken)
            .build();
    return apiProxy.handleRequest(request, TokenResponse.class);
  }

  public ApiResponse<String> ping(String loginToken) throws BadRequestException, IOException {
    var endpoint = endpoints.getPingEndpoint();
    var request =
        new Request.Builder()
            .url(endpoint)
            .get()
            .header("Content-Type", "application/json")
            .header(CookiesAndHeaders.HEADER_LOGIN_TOKEN, loginToken)
            .build();
    return apiProxy.handleRequest(request, String.class);
  }
}
