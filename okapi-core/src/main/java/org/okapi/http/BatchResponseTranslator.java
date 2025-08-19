package org.okapi.http;

import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.List;

public class BatchResponseTranslator {

  public static <T> BatchResponse<T> translate(
      List<HttpBatchClient.Result> results, Class<T> clazz, Gson gson) {
    List<String> clientErrorMessages = new ArrayList<>();
    List<T> decoded = new ArrayList<>();
    int serverErrorCount = 0;

    for (HttpBatchClient.Result result : results) {
      if (!result.isOk()) {
        throw new RuntimeException("Request failed", result.error());
      }

      HttpBatchClient.HttpResponse response = result.response();
      int code = response.code();

      if (code >= 500) {
        serverErrorCount++;
      } else if (code >= 400) {
        clientErrorMessages.add(response.body());
      } else {
        T parsed = gson.fromJson(response.body(), clazz);
        decoded.add(parsed);
      }
    }

    return new BatchResponse<>(decoded, clientErrorMessages, serverErrorCount);
  }

  public static record BatchResponse<T>(
      List<T> results, List<String> clientErrors, int serverErrorCount) {}
}
