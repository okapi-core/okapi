package org.okapi.web.ai.tools.backends.datadog;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.List;
import org.okapi.web.ai.tools.DecodingResult;
import org.okapi.web.ai.tools.signals.GaugeSignal;
import org.okapi.web.ai.tools.signals.HistoSignal;

public class DatadogMetricsDecoder {

  public static DecodingResult<GaugeSignal> mapTimeSeriesResult(String responseJson) {
    if (responseJson == null || responseJson.isBlank()) {
      return new DecodingResult<>("empty Datadog response");
    }

    try {
      JsonObject root = JsonParser.parseString(responseJson).getAsJsonObject();
      JsonArray seriesArray = root.getAsJsonArray("series");
      if (seriesArray == null || seriesArray.isEmpty()) {
        return new DecodingResult<>("no series found in Datadog response");
      }

      for (JsonElement seriesEl : seriesArray) {
        if (!seriesEl.isJsonObject()) {
          continue;
        }
        JsonObject series = seriesEl.getAsJsonObject();
        JsonArray pointList = series.getAsJsonArray("pointlist");
        if (pointList == null) {
          continue;
        }

        List<Long> timestamps = new ArrayList<>();
        List<Double> values = new ArrayList<>();
        for (JsonElement pointEl : pointList) {
          if (!pointEl.isJsonArray()) {
            continue;
          }
          JsonArray point = pointEl.getAsJsonArray();
          if (point.size() < 2 || point.get(1).isJsonNull()) {
            continue;
          }
          try {
            timestamps.add(point.get(0).getAsLong());
            values.add(point.get(1).getAsDouble());
          } catch (Exception ignored) {
            // ignore malformed point
          }
        }

        if (!timestamps.isEmpty() && !values.isEmpty()) {
          return new DecodingResult<>(
              GaugeSignal.builder().timestamps(timestamps).values(values).build());
        }
      }

      return new DecodingResult<>("no valid timeseries points decoded");
    } catch (Exception e) {
      return new DecodingResult<>(e.getMessage());
    }
  }

  public static DecodingResult<HistoSignal> mapHistoSignal(String responseJson) {
    if (responseJson == null || responseJson.isBlank()) {
      return new DecodingResult<>("empty Datadog response");
    }

    try {
      JsonObject root = JsonParser.parseString(responseJson).getAsJsonObject();
      JsonArray seriesArray = root.getAsJsonArray("series");
      if (seriesArray == null || seriesArray.isEmpty()) {
        return new DecodingResult<>("no series found in Datadog response");
      }

      for (JsonElement seriesEl : seriesArray) {
        if (!seriesEl.isJsonObject()) {
          continue;
        }
        JsonObject series = seriesEl.getAsJsonObject();
        JsonArray bucketsArr = series.getAsJsonArray("buckets");
        JsonArray countsArr = series.getAsJsonArray("counts");
        if (bucketsArr == null || countsArr == null) {
          continue;
        }

        List<Double> buckets = new ArrayList<>();
        for (JsonElement el : bucketsArr) {
          if (!el.isJsonNull()) {
            buckets.add(el.getAsDouble());
          }
        }
        List<Long> counts = new ArrayList<>();
        for (JsonElement el : countsArr) {
          if (!el.isJsonNull()) {
            counts.add(el.getAsLong());
          }
        }

        if (!buckets.isEmpty() && !counts.isEmpty()) {
          return new DecodingResult<>(new HistoSignal(null, null, counts, buckets));
        }
      }

      return new DecodingResult<>("no histogram data found");
    } catch (Exception e) {
      return new DecodingResult<>(e.getMessage());
    }
  }
}
