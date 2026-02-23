package org.okapi.metrics.common;

import java.util.*;

public class MetricsPathParser {

  public static Optional<ParsedMetricName> parseMetricsName(String universalName) {
    var parts = universalName.split(":");
    if (parts.length < 2) {
      return Optional.empty();
    }
    var tenantId = parts[0];
    var name = parts[1];
    return Optional.of(new ParsedMetricName(tenantId, name));
  }

  public static Optional<String> tenantId(String path) {
    var startColon = path.indexOf(":");
    if (startColon == -1) {
      return Optional.empty();
    }
    return Optional.of(path.substring(0, startColon));
  }

  public static Optional<MetricsRecord> parse(String path) {
    var startBrace = path.indexOf("{");
    if (startBrace == -1) {
      var partial = parsePartial(path);
      return partial.map(
          partialRecord ->
              new MetricsRecord(
                  partialRecord.tenantId(),
                  partialRecord.serializedMetricPaths(),
                  Collections.emptyMap()));
    }
    var endBrace = path.indexOf("}");
    var name = path.substring(0, startBrace);
    var keyValues = path.substring(1 + startBrace, endBrace);
    var keyValuePairs = keyValues.split(",");
    var tags = new TreeMap<String, String>();
    for (var kvp : keyValuePairs) {
      var split = kvp.split("=");
      if (split.length != 2) continue;
      var key = split[0].trim();
      var value = split[1].trim();
      tags.put(key, value);
    }
    var parsedName = parseMetricsName(name);
    if (parsedName.isEmpty()) return Optional.empty();
    return Optional.of(
        new MetricsRecord(parsedName.get().tenantId(), parsedName.get().name(), tags));
  }

  public static Optional<PartialRecord> parsePartial(String path) {
    var colon = path.indexOf(":");
    if (colon == -1) return Optional.empty();
    var tenantId = path.substring(0, colon);
    var metricPath = path.substring(1 + colon);
    return Optional.of(new PartialRecord(tenantId, metricPath));
  }

  public static Optional<ParsedHashKey> parseHashKey(String hashKey) {
    try {
      // Step 1: Split into segments
      int firstColon = hashKey.indexOf(":");
      if (firstColon == -1) return Optional.empty();
      String tenantId = hashKey.substring(0, firstColon);

      int braceClose = hashKey.indexOf("}", firstColon);
      if (braceClose == -1) return Optional.empty();

      String metricAndTags = hashKey.substring(firstColon + 1, braceClose + 1);
      Optional<MetricsRecord> maybeParsed = parse(tenantId + ":" + metricAndTags);
      if (maybeParsed.isEmpty()) return Optional.empty();
      MetricsRecord metric = maybeParsed.get();

      // Extract resolution and value
      // Expected remaining: :resolution:value
      int afterBrace = braceClose + 1;
      if (hashKey.charAt(afterBrace) != ':') return Optional.empty();
      int secondColon = hashKey.indexOf(":", afterBrace + 1);
      if (secondColon == -1) return Optional.empty();

      String resolution = hashKey.substring(afterBrace + 1, secondColon).trim();
      if (!resolution.equals("h") && !resolution.equals("m") && !resolution.equals("s")) {
        return Optional.empty();
      }

      String valueStr = hashKey.substring(secondColon + 1).trim();
      int value = Integer.parseInt(valueStr);

      return Optional.of(
          new ParsedHashKey(metric.tenantId(), metric.name(), metric.tags(), resolution, value));
    } catch (Exception e) {
      return Optional.empty();
    }
  }

  public record MetricsRecord(String tenantId, String name, Map<String, String> tags) {}

  public record PartialRecord(String tenantId, String serializedMetricPaths) {}

  public record ParsedMetricName(String tenantId, String name) {}

  public record ParsedHashKey(
      String tenantId, String name, Map<String, String> tags, String resolution, long value) {}
}
