package org.okapi.web.ai.tools.backends.datadog;


import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.List;
import org.okapi.web.ai.tools.DecodingResult;
import org.okapi.web.ai.tools.signals.LogsDocument;
import org.okapi.web.ai.tools.signals.LogsSignal;

public class DatadogLogsDecoder {

  public static DecodingResult<LogsSignal> mapJsonRecord(String json) {
    if (json == null || json.isBlank()) {
      return new DecodingResult<>("empty Datadog logs response");
    }
    try {
      JsonObject root = JsonParser.parseString(json).getAsJsonObject();
      var dataArray = root.getAsJsonArray("data");
      if (dataArray == null) {
        return new DecodingResult<>("missing data array in Datadog logs response");
      }
      List<LogsDocument> docs = new ArrayList<>();
      for (JsonElement element : dataArray) {
        if (!element.isJsonObject()) {
          continue;
        }
        JsonObject attributes =
            element.getAsJsonObject().has("attributes")
                ? element.getAsJsonObject().getAsJsonObject("attributes")
                : element.getAsJsonObject();
        if (attributes == null) {
          continue;
        }
        String message = getString(attributes, "message");
        long timestamp = getLong(attributes, "timestamp");
        String level = getString(attributes, "status");
        docs.add(new LogsDocument(message, timestamp, level));
      }

      if (docs.isEmpty()) {
        return new DecodingResult<>("no logs decoded from Datadog response");
      }
      return new DecodingResult<>(LogsSignal.builder().documents(docs).build());
    } catch (Exception e) {
      return new DecodingResult<>(e.getMessage());
    }
  }

  private static String getString(JsonObject obj, String key) {
    if (obj == null) {
      return null;
    }
    JsonElement el = obj.get(key);
    return el == null || el.isJsonNull() ? null : el.getAsString();
  }

  private static long getLong(JsonObject obj, String key) {
    if (obj == null) {
      return 0L;
    }
    JsonElement el = obj.get(key);
    return el == null || el.isJsonNull() ? 0L : el.getAsLong();
  }
}
