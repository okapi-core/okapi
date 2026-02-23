package org.okapi.web.ai.tools.backends.datadog;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.okapi.json.StatefulJsonExtractor;
import org.okapi.web.ai.tools.DecodingResult;
import org.okapi.web.ai.tools.signals.Span;
import org.okapi.web.ai.tools.signals.TracesSignal;

public class DatadogTracesDecoder {

  public static DecodingResult<TracesSignal> mapToSpans(String json) {
    if (json == null || json.isBlank()) {
      return new DecodingResult<>("empty Datadog traces response");
    }
    try {
      StatefulJsonExtractor extractor = new StatefulJsonExtractor(json);
      var dataArrayOpt = extractor.getArray(List.of("data"));
      if (dataArrayOpt.isEmpty()) {
        return new DecodingResult<>("missing data array in Datadog traces response");
      }
      var dataArray = dataArrayOpt.get();
      List<Span> spans = new ArrayList<>();
      for (JsonElement element : dataArray) {
        if (!element.isJsonObject()) {
          continue;
        }
        JsonObject obj = element.getAsJsonObject();
        // attributes block holds actual span fields; fall back to root if absent.
        StatefulJsonExtractor spanExtractor =
            obj.has("attributes")
                ? new StatefulJsonExtractor(obj.get("attributes"))
                : new StatefulJsonExtractor(obj);

        String traceId = spanExtractor.getString(List.of("trace_id")).orElse(null);
        String spanId = spanExtractor.getString(List.of("span_id")).orElse(null);
        String parentId = spanExtractor.getString(List.of("parent_id")).orElse(null);
        long start = spanExtractor.getLong(List.of("start")).orElse(0L);
        long duration = spanExtractor.getLong(List.of("duration")).orElse(0L);
        long end = start + duration;
        String service = spanExtractor.getString(List.of("service")).orElse(null);

        Map<String, Object> attributes = new LinkedHashMap<>();
        if (service != null) {
          attributes.put("service", service);
        }
        // meta fields
        if (obj.has("attributes") && obj.getAsJsonObject("attributes").has("meta")) {
          obj.getAsJsonObject("attributes")
              .getAsJsonObject("meta")
              .entrySet()
              .forEach(e -> attributes.put(e.getKey(), e.getValue().getAsString()));
        }
        // metrics fields
        if (obj.has("attributes") && obj.getAsJsonObject("attributes").has("metrics")) {
          obj.getAsJsonObject("attributes")
              .getAsJsonObject("metrics")
              .entrySet()
              .forEach(
                  e -> {
                    var v = e.getValue();
                    if (v.isJsonPrimitive()) {
                      if (v.getAsJsonPrimitive().isNumber()) {
                        attributes.put(e.getKey(), v.getAsNumber());
                      } else if (v.getAsJsonPrimitive().isBoolean()) {
                        attributes.put(e.getKey(), v.getAsBoolean());
                      } else {
                        attributes.put(e.getKey(), v.getAsString());
                      }
                    } else {
                      attributes.put(e.getKey(), v.toString());
                    }
                  });
        }

        spans.add(
            Span.builder()
                .traceId(traceId)
                .spanId(spanId)
                .parentSpanId(parentId)
                .service(service)
                .start(start)
                .end(end)
                .level(0)
                .attributes(attributes)
                .build());
      }

      if (spans.isEmpty()) {
        return new DecodingResult<>("no spans decoded from Datadog response");
      }

      return new DecodingResult<>(TracesSignal.builder().spans(spans).build());
    } catch (Exception e) {
      return new DecodingResult<>(e.getMessage());
    }
  }

}
