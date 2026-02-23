/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.promql.ch;

import com.clickhouse.client.api.Client;
import com.clickhouse.client.api.query.GenericRecord;
import gg.jte.TemplateOutput;
import gg.jte.output.StringOutput;
import java.util.*;
import org.okapi.ch.ChJteTemplateFiles;
import org.okapi.metrics.ch.ChConstants;
import org.okapi.metrics.ch.ChGetHistoQueryTemplate;
import org.okapi.metrics.ch.ChGetSumQueryTemplate;
import org.okapi.metrics.ch.template.ChMetricTemplateEngine;
import org.okapi.metrics.pojos.results.GaugeScan;
import org.okapi.metrics.pojos.results.Scan;
import org.okapi.metrics.pojos.results.SumScan;
import org.okapi.promql.eval.HistogramSeries;
import org.okapi.promql.eval.ts.RESOLUTION;
import org.okapi.promql.eval.ts.TsClient;

public class ChPromQlTsClient implements TsClient {
  public static final String SERVICE_LABEL = "service";
  private final Client client;
  private final ChMetricTemplateEngine templateEngine;

  public ChPromQlTsClient(Client client, ChMetricTemplateEngine templateEngine) {
    this.client = client;
    this.templateEngine = templateEngine;
  }

  @Override
  public Scan get(String name, Map<String, String> tags, RESOLUTION res, long startMs, long endMs) {
    Map<String, String> tagCopy = tags == null ? new LinkedHashMap<>() : new LinkedHashMap<>(tags);
    String resource = tagCopy.remove(SERVICE_LABEL);
    MetricEventType type = resolveMetricType(resource, name, tagCopy, startMs, endMs);

    return switch (type) {
      case HISTO -> getHistogramSeries(resource, name, tagCopy, startMs, endMs);
      case SUM -> getSumSeries(resource, name, tagCopy, startMs, endMs);
      case GAUGE -> getGaugeSeries(resource, name, tagCopy, startMs, endMs);
    };
  }

  private MetricEventType resolveMetricType(
      String resource, String metric, Map<String, String> tags, long startMs, long endMs) {
    TemplateOutput output = new StringOutput();
    templateEngine.render(
        ChJteTemplateFiles.GET_METRIC_EVENT_TYPE,
        ChMetricEventTypeQueryTemplate.builder()
            .table(ChConstants.TBL_METRIC_EVENTS_META)
            .resource(resource)
            .metric(metric)
            .startMs(startMs)
            .endMs(endMs)
            .tags(tags)
            .build(),
        output);
    var query = output.toString();
    List<GenericRecord> records = client.queryAll(query);
    if (records.isEmpty()) {
      return MetricEventType.GAUGE;
    }
    String eventType = records.getFirst().getString("event_type");
    return MetricEventType.valueOf(eventType);
  }

  private GaugeScan getGaugeSeries(
      String resource, String metric, Map<String, String> tags, long startMs, long endMs) {
    TemplateOutput output = new StringOutput();
    templateEngine.render(
        ChJteTemplateFiles.GET_GAUGE_RAW_SAMPLES,
        ChGetGaugeRawQueryTemplate.builder()
            .table(ChConstants.TBL_GAUGES)
            .resource(resource)
            .metric(metric)
            .startMs(startMs)
            .endMs(endMs)
            .tags(tags)
            .build(),
        output);
    var query = output.toString();
    List<GenericRecord> records = client.queryAll(query);
    var times = new ArrayList<Long>(records.size());
    var values = new ArrayList<Float>(records.size());
    for (var record : records) {
      times.add(record.getLong("ts_ms"));
      values.add((float) record.getDouble("value"));
    }
    return GaugeScan.builder().universalPath(metric).timestamps(times).values(values).build();
  }

  private Scan getSumSeries(
      String resource, String metric, Map<String, String> tags, long startMs, long endMs) {
    var delta = scanSumSamples(resource, metric, tags, startMs, endMs, "DELTA");
    if (!delta.isEmpty()) {
      return toSumScan(metric, delta, false);
    }
    var cumulative = scanSumSamples(resource, metric, tags, startMs, endMs, "CUMULATIVE");
    return toSumScan(metric, cumulative, true);
  }

  private List<SumPoint> scanSumSamples(
      String resource,
      String metric,
      Map<String, String> tags,
      long startMs,
      long endMs,
      String type) {
    TemplateOutput output = new StringOutput();
    templateEngine.render(
        ChJteTemplateFiles.GET_SUM_SAMPLES,
        ChGetSumQueryTemplate.builder()
            .table(ChConstants.TBL_SUM)
            .resource(resource)
            .metric(metric)
            .tags(tags)
            .histoType(type)
            .ts(startMs)
            .te(endMs)
            .build(),
        output);
    var query = output.toString();
    List<GenericRecord> records = client.queryAll(query);
    var points = new ArrayList<SumPoint>(records.size());
    for (var record : records) {
      points.add(
          new SumPoint(
              record.getLong("ts_start_ms"), record.getLong("ts_end_ms"), record.getLong("value")));
    }
    points.sort(Comparator.comparingLong(SumPoint::endMs));
    return points;
  }

  private SumScan toSumScan(String metric, List<SumPoint> points, boolean cumulative) {
    var ts = new ArrayList<Long>(points.size());
    var counts = new ArrayList<Integer>(points.size());
    Long prev = null;
    for (var p : points) {
      ts.add(p.endMs());
      long val = p.value();
      long delta = val;
      if (cumulative && prev != null) {
        delta = val - prev;
        if (delta < 0) delta = val; // counter reset
      }
      counts.add(clampToInt(delta));
      prev = val;
    }
    return SumScan.builder().universalPath(metric).ts(ts).counts(counts).windowSize(0).build();
  }

  private Scan getHistogramSeries(
      String resource, String metric, Map<String, String> tags, long startMs, long endMs) {
    var delta = scanHistoSamples(resource, metric, tags, startMs, endMs, "DELTA");
    if (!delta.isEmpty()) {
      return new HistogramSeries(metric, delta);
    }
    var cumulative = scanHistoSamples(resource, metric, tags, startMs, endMs, "CUMULATIVE");
    if (cumulative.isEmpty()) {
      return new HistogramSeries(metric, List.of());
    }
    return new HistogramSeries(metric, toDeltaHistos(cumulative));
  }

  private List<HistogramSeries.HistogramPoint> scanHistoSamples(
      String resource,
      String metric,
      Map<String, String> tags,
      long startMs,
      long endMs,
      String type) {
    TemplateOutput output = new StringOutput();
    templateEngine.render(
        ChJteTemplateFiles.GET_HISTO_SAMPLES,
        ChGetHistoQueryTemplate.builder()
            .table(ChConstants.TBL_HISTOS)
            .resource(resource)
            .metric(metric)
            .tags(tags)
            .histoType(type)
            .ts(startMs)
            .te(endMs)
            .build(),
        output);
    var query = output.toString();
    List<GenericRecord> records = client.queryAll(query);
    var points = new ArrayList<HistogramSeries.HistogramPoint>(records.size());
    for (var record : records) {
      float[] buckets = readFloatArray(record, "buckets");
      int[] counts = readIntArray(record, "counts");
      points.add(
          new HistogramSeries.HistogramPoint(
              record.getLong("ts_start_ms"), record.getLong("ts_end_ms"), buckets, counts));
    }
    points.sort(Comparator.comparingLong(HistogramSeries.HistogramPoint::endMs));
    return points;
  }

  private List<HistogramSeries.HistogramPoint> toDeltaHistos(
      List<HistogramSeries.HistogramPoint> cumulative) {
    var out = new ArrayList<HistogramSeries.HistogramPoint>(cumulative.size());
    HistogramSeries.HistogramPoint prev = null;
    for (var p : cumulative) {
      int[] counts = p.counts();
      if (prev != null && counts != null && prev.counts() != null) {
        int[] prevCounts = prev.counts();
        if (prevCounts.length == counts.length) {
          int[] delta = new int[counts.length];
          for (int i = 0; i < counts.length; i++) {
            int d = counts[i] - prevCounts[i];
            delta[i] = d < 0 ? counts[i] : d;
          }
          out.add(
              new HistogramSeries.HistogramPoint(p.startMs(), p.endMs(), p.upperBounds(), delta));
        } else {
          out.add(p);
        }
      } else {
        out.add(p);
      }
      prev = p;
    }
    return out;
  }

  private static float[] readFloatArray(GenericRecord record, String key) {
    try {
      return record.getFloatArray(key);
    } catch (Exception e) {
      var list = record.getList(key);
      if (list == null) {
        return new float[0];
      }
      float[] arr = new float[list.size()];
      for (int i = 0; i < list.size(); i++) {
        arr[i] = ((Number) list.get(i)).floatValue();
      }
      return arr;
    }
  }

  private static int[] readIntArray(GenericRecord record, String key) {
    try {
      return record.getIntArray(key);
    } catch (Exception e) {
      var list = record.getList(key);
      if (list == null) {
        return new int[0];
      }
      int[] arr = new int[list.size()];
      for (int i = 0; i < list.size(); i++) {
        arr[i] = ((Number) list.get(i)).intValue();
      }
      return arr;
    }
  }

  private static int clampToInt(long value) {
    if (value > Integer.MAX_VALUE) return Integer.MAX_VALUE;
    if (value < Integer.MIN_VALUE) return Integer.MIN_VALUE;
    return (int) value;
  }

  private enum MetricEventType {
    GAUGE,
    HISTO,
    SUM
  }

  private record SumPoint(long startMs, long endMs, long value) {}
}
