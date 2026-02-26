/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.metrics.otel;

import static org.okapi.clock.OkapiTimeUtils.nanosToMillis;
import static org.okapi.metrics.otel.OtelValueDecoders.*;

import io.opentelemetry.proto.collector.metrics.v1.ExportMetricsServiceRequest;
import io.opentelemetry.proto.common.v1.KeyValue;
import io.opentelemetry.proto.metrics.v1.*;
import java.util.*;
import java.util.stream.Collectors;
import org.okapi.bytes.OkapiBytes;
import org.okapi.collections.OkapiLists;
import org.okapi.collections.OkapiMaps;
import org.okapi.otel.ResourceAttributesReader;
import org.okapi.rest.metrics.Exemplar;
import org.okapi.rest.metrics.ExportMetricsRequest;
import org.okapi.rest.metrics.MetricType;
import org.okapi.rest.metrics.payloads.Gauge.GaugeBuilder;
import org.okapi.rest.metrics.payloads.Histo;
import org.okapi.rest.metrics.payloads.HistoPoint;
import org.okapi.rest.metrics.payloads.SUM_TEMPORALITY;
import org.okapi.rest.metrics.payloads.SumPoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Converts OTLP ExportMetricsServiceRequest (protobuf) into Okapi ExportMetricsRequest(s).
 *
 * <p>Notes: - Handles only Gauge, Sum, and Histogram types. - Groups datapoints by the full tag set
 * (resource + scope + datapoint attributes). - Converts OTLP timestamps (nanos) to epoch
 * milliseconds expected by Okapi.
 */
@Component
public final class OtelConverter {

  private final OtelConverterConfig config;

  public OtelConverter() {
    this(OtelConverterConfig.defaultConfig());
  }

  @Autowired
  public OtelConverter(OtelConverterConfig config) {
    this.config = config == null ? OtelConverterConfig.defaultConfig() : config;
  }

  private Map<String, String> toTagMap(List<KeyValue> attrs) {
    Map<String, String> out = new LinkedHashMap<>();
    for (KeyValue kv : attrs) {
      if (kv == null || kv.getKey().isEmpty()) continue;
      if (isExcludedTag(kv.getKey())) continue;
      out.put(kv.getKey(), anyValueToString(kv.getValue()));
    }
    return out;
  }

  private static Map<String, String> mergeTags(Map<String, String> a, Map<String, String> b) {
    return OkapiMaps.mergeLeft(a, b, TreeMap::new);
  }

  private static String canonicalKey(Map<String, String> tags) {
    return tags.entrySet().stream()
        .sorted(Map.Entry.comparingByKey())
        .map(e -> e.getKey() + "=" + Objects.toString(e.getValue(), ""))
        .collect(Collectors.joining("|"));
  }

  private boolean isExcludedTag(String key) {
    if (key == null || key.isEmpty()) return false;
    if (config.getExcludeTags().contains(key)) return true;
    for (String prefix : config.getExcludeTagPrefixes()) {
      if (key.startsWith(prefix)) return true;
    }
    return false;
  }

  /** Convert an OTLP request into one or more Okapi requests. */
  public List<ExportMetricsRequest> toOkapiRequests(ExportMetricsServiceRequest otlp) {
    if (otlp == null) return Collections.emptyList();
    List<ExportMetricsRequest> out = new ArrayList<>();
    for (ResourceMetrics rm : otlp.getResourceMetricsList()) {
      out.addAll(convertResourceMetrics(rm));
    }
    return out;
  }

  private List<ExportMetricsRequest> convertResourceMetrics(ResourceMetrics rm) {
    Map<String, String> resourceTags =
        rm.hasResource() ? toTagMap(rm.getResource().getAttributesList()) : Map.of();
    var resourceName =
        rm.hasResource() ? ResourceAttributesReader.getSvc(rm.getResource()).orElse(null) : null;
    List<ExportMetricsRequest> out = new ArrayList<>();
    for (ScopeMetrics sm : rm.getScopeMetricsList()) {
      out.addAll(convertScopeMetrics(resourceTags, sm, resourceName));
    }
    return out;
  }

  private List<ExportMetricsRequest> convertScopeMetrics(
      Map<String, String> resourceTags, ScopeMetrics sm, String resourceName) {
    Map<String, String> scopeTags = new LinkedHashMap<>(resourceTags);
    if (sm.hasScope()) {
      scopeTags.putAll(toTagMap(sm.getScope().getAttributesList()));
    }

    List<ExportMetricsRequest> out = new ArrayList<>();
    for (Metric m : sm.getMetricsList()) {
      out.addAll(convertMetric(scopeTags, m, resourceName));
    }
    return out;
  }

  private List<ExportMetricsRequest> convertMetric(
      Map<String, String> baseTags, Metric m, String resourceName) {
    if (m.hasGauge()) {
      return convertGauge(m.getName(), baseTags, m.getGauge(), resourceName);
    } else if (m.hasSum()) {
      return convertSum(m.getName(), baseTags, m.getSum(), resourceName);
    } else if (m.hasHistogram()) {
      return convertHistogram(m.getName(), baseTags, m.getHistogram(), resourceName);
    }
    return Collections.emptyList();
  }

  public Exemplar fromOtelExemplarToOkapiExemplar(
      String metric,
      Map<String, String> tags,
      io.opentelemetry.proto.metrics.v1.Exemplar otelExemplar) {
    var val = OtelValueDecoders.decodeExemplarAsDouble(otelExemplar);
    var attributes =
        otelExemplar.getFilteredAttributesList().stream()
            .map(OtelValueDecoders::decodeKv)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .toList();
    var traceId = OkapiBytes.encodeAsHex(otelExemplar.getTraceId().toByteArray());
    var spanId = OkapiBytes.encodeAsHex(otelExemplar.getSpanId().toByteArray());
    var tsNanos = otelExemplar.getTimeUnixNano();
    var builder =
        Exemplar.builder()
            .spanId(spanId)
            .tsNanos(tsNanos)
            .metric(metric)
            .tags(tags)
            .traceId(traceId)
            .kv(attributes);
    val.ifPresent(builder::measurement);
    return builder.build();
  }

  public List<Exemplar> collectExemplar(
      String metric, Map<String, String> tags, NumberDataPoint numberDataPoint) {
    return numberDataPoint.getExemplarsList().stream()
        .map((e) -> this.fromOtelExemplarToOkapiExemplar(metric, tags, e))
        .toList();
  }

  public List<Exemplar> collectExemplar(
      String metric, Map<String, String> tags, HistogramDataPoint histogramDataPoint) {
    return histogramDataPoint.getExemplarsList().stream()
        .map((e) -> this.fromOtelExemplarToOkapiExemplar(metric, tags, e))
        .toList();
  }

  private List<ExportMetricsRequest> convertGauge(
      String metricName, Map<String, String> baseTags, Gauge g, String resourceName) {
    // Group points by tags
    Map<String, Map<String, String>> tagKeyToTags = new HashMap<>();
    Map<String, List<Long>> tsByKey = new HashMap<>();
    Map<String, List<Float>> valsByKey = new HashMap<>();
    Map<String, List<Exemplar>> exemplarsByKey = new HashMap<>();
    for (NumberDataPoint p : g.getDataPointsList()) {
      Map<String, String> tags = mergeTags(baseTags, toTagMap(p.getAttributesList()));
      String key = canonicalKey(tags);
      tagKeyToTags.putIfAbsent(key, tags);
      tsByKey
          .computeIfAbsent(key, OkapiLists::keyToEmptyArrayList)
          .add(nanosToMillis(p.getTimeUnixNano()));
      valsByKey
          .computeIfAbsent(key, OkapiLists::keyToEmptyArrayList)
          .add(OtelValueDecoders.extractNumberAsFloat(p));
      var exemplar = collectExemplar(metricName, tags, p);
      exemplarsByKey.computeIfAbsent(key, OkapiLists::keyToEmptyArrayList).addAll(exemplar);
    }

    List<ExportMetricsRequest> out = new ArrayList<>();
    for (String key : tagKeyToTags.keySet()) {
      var tsList = tsByKey.getOrDefault(key, List.of());
      var valList = valsByKey.getOrDefault(key, List.of());
      GaugeBuilder gauge =
          org.okapi.rest.metrics.payloads.Gauge.builder()
              .ts(tsList)
              .exemplars(Collections.unmodifiableList(exemplarsByKey.get(key)))
              .value(valList);
      out.add(
          ExportMetricsRequest.builder()
              .resource(resourceName)
              .metricName(metricName)
              .tags(tagKeyToTags.get(key))
              .type(MetricType.GAUGE)
              .gauge(gauge.build())
              .build());
    }
    return out;
  }

  private List<ExportMetricsRequest> convertSum(
      String metricName, Map<String, String> baseTags, Sum s, String resourceName) {
    SUM_TEMPORALITY SUMTYPE =
        switch (s.getAggregationTemporality()) {
          case AGGREGATION_TEMPORALITY_CUMULATIVE -> SUM_TEMPORALITY.CUMULATIVE;
          case AGGREGATION_TEMPORALITY_DELTA -> SUM_TEMPORALITY.DELTA;
          case AGGREGATION_TEMPORALITY_UNSPECIFIED -> SUM_TEMPORALITY.DELTA; // fallback
          default -> SUM_TEMPORALITY.DELTA;
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
      ptsByKey.computeIfAbsent(key, OkapiLists::keyToEmptyArrayList).add(pt);
    }

    List<ExportMetricsRequest> out = new ArrayList<>();
    for (String key : tagKeyToTags.keySet()) {
      var points = ptsByKey.getOrDefault(key, List.of());
      var sumPayload =
          org.okapi.rest.metrics.payloads.Sum.builder()
              .temporality(SUMTYPE)
              .sumPoints(points)
              .build();
      out.add(
          ExportMetricsRequest.builder()
              .resource(resourceName)
              .metricName(metricName)
              .tags(tagKeyToTags.get(key))
              .type(MetricType.COUNTER)
              .sum(sumPayload)
              .build());
    }
    return out;
  }

  private List<ExportMetricsRequest> convertHistogram(
      String metricName, Map<String, String> baseTags, Histogram otelHisto, String resourceName) {
    // Group datapoints by tags
    Map<String, Map<String, String>> tagKeyToTags = new HashMap<>();
    Map<String, List<HistoPoint>> ptsByKey = new HashMap<>();
    Map<String, List<Exemplar>> exemplarsByKey = new HashMap<>();
    for (var p : otelHisto.getDataPointsList()) {
      Map<String, String> tags = mergeTags(baseTags, toTagMap(p.getAttributesList()));
      String key = canonicalKey(tags);
      tagKeyToTags.putIfAbsent(key, tags);

      HistoPoint pt = new HistoPoint();
      pt.setStart(nanosToMillis(p.getStartTimeUnixNano()));
      pt.setEnd(nanosToMillis(p.getTimeUnixNano()));
      var translatedTemporality =
          switch (otelHisto.getAggregationTemporality()) {
            case AGGREGATION_TEMPORALITY_DELTA -> HistoPoint.TEMPORALITY.DELTA;
            case AGGREGATION_TEMPORALITY_CUMULATIVE -> HistoPoint.TEMPORALITY.CUMULATIVE;
            case AGGREGATION_TEMPORALITY_UNSPECIFIED -> HistoPoint.TEMPORALITY.DELTA; // fallback
            default -> HistoPoint.TEMPORALITY.DELTA;
          };
      pt.setTemporality(translatedTemporality);

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
      ptsByKey.computeIfAbsent(key, OkapiLists::keyToEmptyArrayList).add(pt);

      var exemplars = collectExemplar(metricName, tags, p);
      exemplarsByKey.computeIfAbsent(key, OkapiLists::keyToEmptyArrayList).addAll(exemplars);
    }

    List<ExportMetricsRequest> out = new ArrayList<>();
    for (String key : tagKeyToTags.keySet()) {
      Histo histo = Histo.builder().histoPoints(ptsByKey.getOrDefault(key, List.of())).build();
      out.add(
          ExportMetricsRequest.builder()
              .resource(resourceName)
              .metricName(metricName)
              .tags(tagKeyToTags.get(key))
              .type(MetricType.HISTO)
              .histo(histo)
              .build());
    }
    return out;
  }
}
