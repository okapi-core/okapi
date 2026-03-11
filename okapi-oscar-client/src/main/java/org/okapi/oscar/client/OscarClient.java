package org.okapi.oscar.client;

import com.google.gson.Gson;
import java.io.IOException;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.okapi.ingester.client.ProxyResponseTranslator;
import org.okapi.rest.chat.ChatHistoryResponse;
import org.okapi.rest.chat.ChatResponse;
import org.okapi.rest.chat.PostMessageRequest;

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

  public ChatResponse postMessage(String sessionId, PostMessageRequest request) {
    RequestBody body = RequestBody.create(gson.toJson(request).getBytes());
    Request httpRequest =
        new Request.Builder()
            .url(endpoint + "/api/v1/chat/" + sessionId)
            .header("Content-Type", "application/json")
            .post(body)
            .build();
    try (var response = client.newCall(httpRequest).execute()) {
      return translator.translateResponse(response, ChatResponse.class);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public ChatHistoryResponse getHistory(String sessionId, Long from, Long to) {
    HttpUrl.Builder urlBuilder =
        HttpUrl.parse(endpoint + "/api/v1/chat/messages/" + sessionId)
            .newBuilder()
            .addQueryParameter("from", String.valueOf(from));
    if (to != null) {
      urlBuilder.addQueryParameter("to", String.valueOf(to));
    }
    Request httpRequest = new Request.Builder().url(urlBuilder.build()).get().build();
    try (var response = client.newCall(httpRequest).execute()) {
      return translator.translateResponse(response, ChatHistoryResponse.class);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
