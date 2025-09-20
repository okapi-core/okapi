package org.okapi.metrics.search;

import org.okapi.metrics.common.MetricsPathParser;

import java.util.*;
import java.util.regex.Pattern;

public class MetricsSearcher {

  public static List<MetricsPathParser.MetricsRecord> searchMatchingMetrics(
      String tenantId, Collection<String> univPaths, String pattern) {

    PatternInfo patternInfo = PatternInfo.parse(pattern);
    List<MetricsPathParser.MetricsRecord> results = new ArrayList<>();

    for (String path : univPaths) {
      var maybeRecord = MetricsPathParser.parse(path);
      if (maybeRecord.isEmpty()) continue;
      var record = maybeRecord.get();

      if (!tenantId.equals(record.tenantId())) continue;
      if (!patternInfo.metricNameMatches(record.name())) continue;
      if (!patternInfo.labelFiltersMatch(record.tags())) continue;
      results.add(record);
    }

    return results;
  }

  public static boolean isAMatch(String tenantId, String path, PatternInfo patternInfo) {
    var maybeRecord = MetricsPathParser.parse(path);
    if (maybeRecord.isEmpty()) return false;
    var record = maybeRecord.get();

    if (!tenantId.equals(record.tenantId())) return false;
    if (!patternInfo.metricNameMatches(record.name())) return false;
    if (!patternInfo.labelFiltersMatch(record.tags())) return false;
    return true;
  }

  public static class PatternInfo {
    private final Pattern metricNamePattern;
    private final List<LabelFilter> labelFilters;

    private PatternInfo(Pattern metricNamePattern, List<LabelFilter> labelFilters) {
      this.metricNamePattern = metricNamePattern;
      this.labelFilters = labelFilters;
    }

    public static PatternInfo parse(String input) {
      String metricPattern = null;
      List<LabelFilter> labelFilters = new ArrayList<>();

      int braceStart = input.indexOf('{');
      int braceEnd = input.indexOf('}');

      if (braceStart >= 0 && braceEnd > braceStart) {
        metricPattern = input.substring(0, braceStart).trim();
        String labelsStr = input.substring(braceStart + 1, braceEnd);

        for (String part : labelsStr.split(",")) {
          if (part.isBlank()) continue;

          String[] kv = part.split("=");
          if (kv.length != 2) continue;

          String rawKey = kv[0].trim();
          String rawVal = kv[1].trim().replaceAll("^\"|\"$", ""); // remove quotes

          boolean keyHasWildcard = rawKey.endsWith("*");
          boolean valHasWildcard = rawVal.endsWith("*");

          if (keyHasWildcard && valHasWildcard) {
            throw new IllegalArgumentException(
                "Wildcard in both key and value is not allowed: " + part);
          }

          if (keyHasWildcard) {
            labelFilters.add(LabelFilter.prefixKey(rawKey.substring(0, rawKey.length() - 1)));
          } else if (rawVal.equals("*")) {
            labelFilters.add(LabelFilter.anyValue(rawKey));
          } else if (valHasWildcard) {
            labelFilters.add(
                LabelFilter.prefixValue(rawKey, rawVal.substring(0, rawVal.length() - 1)));
          } else {
            labelFilters.add(LabelFilter.exact(rawKey, rawVal));
          }
        }
      } else {
        metricPattern = input.trim();
      }

      Pattern compiledMetricPattern = Pattern.compile(wildcardToRegex(metricPattern));
      return new PatternInfo(compiledMetricPattern, labelFilters);
    }

    boolean metricNameMatches(String name) {
      return metricNamePattern.matcher(name).matches();
    }

    boolean labelFiltersMatch(Map<String, String> labels) {
      for (LabelFilter filter : labelFilters) {
        if (!filter.matches(labels)) return false;
      }
      return true;
    }
  }

  private static class LabelFilter {
    enum Type {
      EXACT,
      VALUE_PREFIX,
      KEY_PREFIX,
      ANY
    }

    final Type type;
    final String key;
    final String value;

    private LabelFilter(Type type, String key, String value) {
      this.type = type;
      this.key = key;
      this.value = value;
    }

    static LabelFilter exact(String key, String value) {
      return new LabelFilter(Type.EXACT, key, value);
    }

    static LabelFilter prefixValue(String key, String prefix) {
      return new LabelFilter(Type.VALUE_PREFIX, key, prefix);
    }

    static LabelFilter prefixKey(String prefix) {
      return new LabelFilter(Type.KEY_PREFIX, prefix, null);
    }

    static LabelFilter anyValue(String key) {
      return new LabelFilter(Type.ANY, key, null);
    }

    boolean matches(Map<String, String> labels) {
      return switch (type) {
        case EXACT -> labels.containsKey(key) && labels.get(key).equals(value);
        case VALUE_PREFIX -> labels.containsKey(key) && labels.get(key).startsWith(value);
        case KEY_PREFIX -> labels.keySet().stream().anyMatch(k -> k.startsWith(key));
        case ANY -> labels.containsKey(key);
      };
    }
  }

  private static String wildcardToRegex(String pattern) {
    if (pattern == null || pattern.isEmpty()) return ".*"; // match all
    return "^" + Pattern.quote(pattern).replace("*", "\\E.*\\Q") + "$";
  }
}
