package org.okapi.metrics.otel;

import io.opentelemetry.proto.collector.metrics.v1.ExportMetricsServiceRequest;
import io.opentelemetry.proto.common.v1.AnyValue;
import io.opentelemetry.proto.common.v1.KeyValue;
import io.opentelemetry.proto.metrics.v1.Gauge;
import io.opentelemetry.proto.metrics.v1.Histogram;
import io.opentelemetry.proto.metrics.v1.HistogramDataPoint;
import io.opentelemetry.proto.metrics.v1.Metric;
import io.opentelemetry.proto.metrics.v1.NumberDataPoint;
import io.opentelemetry.proto.metrics.v1.ResourceMetrics;
import io.opentelemetry.proto.metrics.v1.ScopeMetrics;
import io.opentelemetry.proto.metrics.v1.Sum;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.okapi.rest.metrics.ExportMetricsRequest;
import org.okapi.rest.metrics.MetricType;
import org.okapi.rest.metrics.payloads.Gauge.GaugeBuilder;
import org.okapi.rest.metrics.payloads.Histo;
import org.okapi.rest.metrics.payloads.HistoPoint;
import org.okapi.rest.metrics.payloads.SumPoint;
import org.okapi.rest.metrics.payloads.SumType;

/**
 * Converts OTLP ExportMetricsServiceRequest (protobuf) into Okapi ExportMetricsRequest(s).
 *
 * <p>Notes: - Handles only Gauge, Sum, and Histogram types. - Groups datapoints by the full tag set
 * (resource + scope + datapoint attributes). - Converts OTLP timestamps (nanos) to epoch
 * milliseconds expected by Okapi.
 */
public final class OtelConverter {

  /** Convert an OTLP request into one or more Okapi requests. */
  public List<ExportMetricsRequest> toOkapiRequests(
      String tenantId, ExportMetricsServiceRequest otlp) {
    if (otlp == null) return Collections.emptyList();
    List<ExportMetricsRequest> out = new ArrayList<>();
    for (ResourceMetrics rm : otlp.getResourceMetricsList()) {
      out.addAll(convertResourceMetrics(tenantId, rm));
    }
    return out;
  }

  private List<ExportMetricsRequest> convertResourceMetrics(String tenantId, ResourceMetrics rm) {
    Map<String, String> resourceTags =
        rm.hasResource() ? toTagMap(rm.getResource().getAttributesList()) : Map.of();
    List<ExportMetricsRequest> out = new ArrayList<>();
    for (ScopeMetrics sm : rm.getScopeMetricsList()) {
      out.addAll(convertScopeMetrics(tenantId, resourceTags, sm));
    }
    return out;
  }

  private List<ExportMetricsRequest> convertScopeMetrics(
      String tenantId, Map<String, String> resourceTags, ScopeMetrics sm) {
    Map<String, String> scopeTags = new LinkedHashMap<>(resourceTags);
    if (sm.hasScope()) {
      scopeTags.putAll(toTagMap(sm.getScope().getAttributesList()));
    }

    List<ExportMetricsRequest> out = new ArrayList<>();
    for (Metric m : sm.getMetricsList()) {
      out.addAll(convertMetric(tenantId, scopeTags, m));
    }
    return out;
  }

  private List<ExportMetricsRequest> convertMetric(
      String tenantId, Map<String, String> baseTags, Metric m) {
    if (m.hasGauge()) {
      return convertGauge(tenantId, m.getName(), baseTags, m.getGauge());
    } else if (m.hasSum()) {
      return convertSum(tenantId, m.getName(), baseTags, m.getSum());
    } else if (m.hasHistogram()) {
      return convertHistogram(tenantId, m.getName(), baseTags, m.getHistogram());
    }
    return Collections.emptyList();
  }

  private List<ExportMetricsRequest> convertGauge(
      String tenantId, String metricName, Map<String, String> baseTags, Gauge g) {
    // Group points by tags
    Map<String, Map<String, String>> tagKeyToTags = new HashMap<>();
    Map<String, List<Long>> tsByKey = new HashMap<>();
    Map<String, List<Float>> valsByKey = new HashMap<>();

    for (NumberDataPoint p : g.getDataPointsList()) {
      Map<String, String> tags = mergeTags(baseTags, toTagMap(p.getAttributesList()));
      String key = canonicalKey(tags);
      tagKeyToTags.putIfAbsent(key, tags);
      tsByKey.computeIfAbsent(key, k -> new ArrayList<>()).add(nanosToMillis(p.getTimeUnixNano()));
      valsByKey.computeIfAbsent(key, k -> new ArrayList<>()).add(extractNumberAsFloat(p));
    }

    List<ExportMetricsRequest> out = new ArrayList<>();
    for (String key : tagKeyToTags.keySet()) {
      var tsList = tsByKey.getOrDefault(key, List.of());
      var valList = valsByKey.getOrDefault(key, List.of());
      long[] ts = tsList.stream().mapToLong(Long::longValue).toArray();
      float[] values = new float[valList.size()];
      for (int i = 0; i < valList.size(); i++) values[i] = valList.get(i);

      GaugeBuilder gauge = org.okapi.rest.metrics.payloads.Gauge.builder().ts(ts).value(values);

      out.add(
          ExportMetricsRequest.builder()
              .tenantId(tenantId)
              .metricName(metricName)
              .tags(tagKeyToTags.get(key))
              .type(MetricType.GAUGE)
              .gauge(gauge.build())
              .build());
    }
    return out;
  }

  private List<ExportMetricsRequest> convertSum(
      String tenantId, String metricName, Map<String, String> baseTags, Sum s) {
    SumType sumType =
        switch (s.getAggregationTemporality()) {
          case AGGREGATION_TEMPORALITY_CUMULATIVE -> SumType.CUMULATIVE;
          case AGGREGATION_TEMPORALITY_DELTA -> SumType.DELTA;
          case AGGREGATION_TEMPORALITY_UNSPECIFIED -> SumType.DELTA; // fallback
          default -> SumType.DELTA;
        };

    // Group datapoints by tags
    Map<String, Map<String, String>> tagKeyToTags = new HashMap<>();
    Map<String, List<SumPoint>> ptsByKey = new HashMap<>();
    for (NumberDataPoint p : s.getDataPointsList()) {
      Map<String, String> tags = mergeTags(baseTags, toTagMap(p.getAttributesList()));
      String key = canonicalKey(tags);
      tagKeyToTags.putIfAbsent(key, tags);
      SumPoint pt = new SumPoint();
      pt.setStart(nanosToMillis(p.getStartTimeUnixNano()));
      pt.setEnd(nanosToMillis(p.getTimeUnixNano()));
      pt.setSum(extractNumberAsInt(p));
      ptsByKey.computeIfAbsent(key, k -> new ArrayList<>()).add(pt);
    }

    List<ExportMetricsRequest> out = new ArrayList<>();
    for (String key : tagKeyToTags.keySet()) {
      var points = ptsByKey.getOrDefault(key, List.of());
      var sumPayload =
          org.okapi.rest.metrics.payloads.Sum.builder().sumType(sumType).sumPoints(points).build();
      out.add(
          ExportMetricsRequest.builder()
              .tenantId(tenantId)
              .metricName(metricName)
              .tags(tagKeyToTags.get(key))
              .type(MetricType.COUNTER)
              .sum(sumPayload)
              .build());
    }
    return out;
  }

  private List<ExportMetricsRequest> convertHistogram(
      String tenantId, String metricName, Map<String, String> baseTags, Histogram h) {
    // Group datapoints by tags
    Map<String, Map<String, String>> tagKeyToTags = new HashMap<>();
    Map<String, List<HistoPoint>> ptsByKey = new HashMap<>();

    for (HistogramDataPoint p : h.getDataPointsList()) {
      Map<String, String> tags = mergeTags(baseTags, toTagMap(p.getAttributesList()));
      String key = canonicalKey(tags);
      tagKeyToTags.putIfAbsent(key, tags);

      HistoPoint pt = new HistoPoint();
      pt.setStart(nanosToMillis(p.getStartTimeUnixNano()));
      pt.setEnd(nanosToMillis(p.getTimeUnixNano()));

      // Bounds: double[] -> float[]
      float[] buckets = new float[p.getExplicitBoundsCount()];
      for (int i = 0; i < buckets.length; i++) {
        buckets[i] = (float) p.getExplicitBounds(i);
      }
      pt.setBuckets(buckets);

      // Counts: uint64[] -> int[] with clamping
      int[] counts = new int[p.getBucketCountsCount()];
      for (int i = 0; i < counts.length; i++) {
        counts[i] = clampToInt(p.getBucketCounts(i));
      }
      pt.setBucketCounts(counts);

      ptsByKey.computeIfAbsent(key, k -> new ArrayList<>()).add(pt);
    }

    List<ExportMetricsRequest> out = new ArrayList<>();
    for (String key : tagKeyToTags.keySet()) {
      Histo histo = Histo.builder().histoPoints(ptsByKey.getOrDefault(key, List.of())).build();
      out.add(
          ExportMetricsRequest.builder()
              .tenantId(tenantId)
              .metricName(metricName)
              .tags(tagKeyToTags.get(key))
              .type(MetricType.HISTO)
              .histo(histo)
              .build());
    }
    return out;
  }

  private static Map<String, String> toTagMap(List<KeyValue> attrs) {
    if (attrs == null || attrs.isEmpty()) return Map.of();
    Map<String, String> out = new LinkedHashMap<>();
    for (KeyValue kv : attrs) {
      if (kv == null || kv.getKey().isEmpty()) continue;
      out.put(kv.getKey(), anyValueToString(kv.getValue()));
    }
    return out;
  }

  private static String anyValueToString(AnyValue v) {
    if (v == null) return "";
    return switch (v.getValueCase()) {
      case STRING_VALUE -> v.getStringValue();
      case BOOL_VALUE -> Boolean.toString(v.getBoolValue());
      case INT_VALUE -> Long.toString(v.getIntValue());
      case DOUBLE_VALUE -> Double.toString(v.getDoubleValue());
      case ARRAY_VALUE ->
          v.getArrayValue().getValuesList().stream()
              .map(OtelConverter::anyValueToString)
              .collect(Collectors.joining(",", "[", "]"));
      case KVLIST_VALUE ->
          v.getKvlistValue().getValuesList().stream()
              .map(kv -> kv.getKey() + "=" + anyValueToString(kv.getValue()))
              .collect(Collectors.joining(",", "{", "}"));
      case BYTES_VALUE -> Arrays.toString(v.getBytesValue().toByteArray());
      case VALUE_NOT_SET -> "";
    };
  }

  private static Map<String, String> mergeTags(Map<String, String> a, Map<String, String> b) {
    if ((a == null || a.isEmpty()) && (b == null || b.isEmpty())) return Map.of();
    if (a == null || a.isEmpty()) return new LinkedHashMap<>(b);
    if (b == null || b.isEmpty()) return new LinkedHashMap<>(a);
    Map<String, String> out = new LinkedHashMap<>(a);
    out.putAll(b);
    return out;
  }

  private static String canonicalKey(Map<String, String> tags) {
    if (tags == null || tags.isEmpty()) return "";
    return tags.entrySet().stream()
        .sorted(Comparator.comparing(Map.Entry::getKey))
        .map(e -> e.getKey() + "=" + Objects.toString(e.getValue(), ""))
        .collect(Collectors.joining("|"));
  }

  private static long nanosToMillis(long nanos) {
    return TimeUnit.NANOSECONDS.toMillis(nanos);
  }

  private static float extractNumberAsFloat(NumberDataPoint p) {
    return switch (p.getValueCase()) {
      case AS_DOUBLE -> (float) p.getAsDouble();
      case AS_INT -> (float) p.getAsInt();
      case VALUE_NOT_SET -> 0.0f;
    };
  }

  private static int extractNumberAsInt(NumberDataPoint p) {
    switch (p.getValueCase()) {
      case AS_DOUBLE:
        double d = p.getAsDouble();
        long rounded = Math.round(d);
        return clampToInt(rounded);
      case AS_INT:
        return clampToInt(p.getAsInt());
      case VALUE_NOT_SET:
        return 0;
      default:
        return 0;
    }
  }

  private static int clampToInt(long value) {
    if (value > Integer.MAX_VALUE) return Integer.MAX_VALUE;
    if (value < Integer.MIN_VALUE) return Integer.MIN_VALUE;
    return (int) value;
  }
}
