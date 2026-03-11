package org.okapi.oscar.client;

import com.google.gson.Gson;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.okapi.ingester.client.ProxyResponseTranslator;
import org.okapi.rest.chat.ChatHistoryResponse;
import org.okapi.rest.chat.ChatMessageUpdatesResponse;
import org.okapi.rest.chat.ChatResponse;
import org.okapi.rest.chat.GetHistoryRequest;
import org.okapi.rest.chat.PostMessageRequest;
import org.okapi.rest.session.CreateSessionRequest;
import org.okapi.rest.session.SessionMetaResponse;

import java.io.IOException;

public class OscarClient {

  private final String endpoint;
  private final OkHttpClient client;
  private final ProxyResponseTranslator translator;
  private final Gson gson = new Gson();

  public OscarClient(String endpoint, OkHttpClient client, ProxyResponseTranslator translator) {
    this.endpoint = endpoint;
    this.client = client;
    this.translator = translator;
  }

  private <T> T postRequest(String path, Object requestBody, Class<T> clazz) {
    RequestBody body = RequestBody.create(gson.toJson(requestBody).getBytes());
    Request request =
        new Request.Builder()
            .url(safeUrlConcat(endpoint, path))
            .header("Content-Type", "application/json")
            .post(body)
            .build();
    try (var response = client.newCall(request).execute()) {
      return translator.translateResponse(response, clazz);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private <T> T postEmpty(String path, Class<T> clazz) {
    Request request =
        new Request.Builder()
            .url(safeUrlConcat(endpoint, path))
            .post(RequestBody.create(new byte[0]))
            .build();
    try (var response = client.newCall(request).execute()) {
      return translator.translateResponse(response, clazz);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private <T> T getRequest(String path, Class<T> clazz) {
    Request request =
        new Request.Builder()
            .url(safeUrlConcat(endpoint, path))
            .get()
            .build();
    try (var response = client.newCall(request).execute()) {
      return translator.translateResponse(response, clazz);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public String safeUrlConcat(String endpoint, String path) {
    var sb = new StringBuilder();
    var arr = endpoint.toCharArray();
    int n = arr.length;
    if (endpoint.endsWith("/")) {
      n--;
    }
    for (int i = 0; i < n; i++) {
      sb.append(arr[i]);
    }
    if (path.startsWith("/")) {
      sb.append(path);
    } else {
      sb.append('/').append(path);
    }
    return sb.toString();
  }

  public ChatResponse postMessage(String sessionId, PostMessageRequest request) {
    return postRequest("/api/v1/chat/" + sessionId, request, ChatResponse.class);
  }

  public ChatHistoryResponse getHistory(String sessionId, GetHistoryRequest rq) {
    return postRequest("/api/v1/chat/history/" + sessionId, rq, ChatHistoryResponse.class);
  }

  public ChatMessageUpdatesResponse getUpdates(String sessionId) {
    return getRequest("/api/v1/chat/" + sessionId + "/updates", ChatMessageUpdatesResponse.class);
  }

  public SessionMetaResponse createSession(CreateSessionRequest request) {
    return postRequest("/api/v1/sessions", request, SessionMetaResponse.class);
  }

  public SessionMetaResponse getSessionMeta(String sessionId) {
    return getRequest("/api/v1/sessions/" + sessionId + "/meta", SessionMetaResponse.class);
  }

  public SessionMetaResponse pingSession(String sessionId) {
    return postEmpty("/api/v1/sessions/" + sessionId + "/ping", SessionMetaResponse.class);
  }
}
