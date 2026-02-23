package org.okapi.metrics.ch;

import com.clickhouse.client.api.Client;
import com.clickhouse.client.api.query.GenericRecord;
import com.google.common.primitives.Floats;
import com.google.common.primitives.Ints;
import gg.jte.TemplateOutput;
import gg.jte.output.StringOutput;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.okapi.ch.ChJteTemplateFiles;
import org.okapi.metrics.ch.template.ChMetricTemplateEngine;
import org.okapi.ds.HistogramMerger;
import org.okapi.ds.TwoAtATimeMerger;
import org.okapi.rest.metrics.query.CannedResponses;
import org.okapi.rest.metrics.query.GetHistogramResponse;
import org.okapi.rest.metrics.query.GetMetricsRequest;
import org.okapi.rest.metrics.query.GetMetricsResponse;
import org.okapi.rest.metrics.query.HistoQueryConfig;
import org.okapi.rest.metrics.query.Histogram;

/** Handles histogram query execution and aggregation. */
public class HistogramQueryProcessor {
  private final Client client;
  private final ChMetricTemplateEngine templateEngine;

  public HistogramQueryProcessor(Client client, ChMetricTemplateEngine templateEngine) {
    this.client = client;
    this.templateEngine = templateEngine;
  }

  public GetMetricsResponse getHistoRes(GetMetricsRequest query) {
    var ts = query.getStart();
    var te = query.getEnd();
    var histoType =
        switch (query.getHistoQueryConfig().getTemporality()) {
          case DELTA -> ChHistoSample.HISTO_TYPE.DELTA;
          case CUMULATIVE -> ChHistoSample.HISTO_TYPE.CUMULATIVE;
          case MERGED -> ChHistoSample.HISTO_TYPE.DELTA;
        };
    var readings =
        scanSamples(ts, te, query.getSvc(), query.getMetric(), query.getTags(), histoType);
    if (query.getHistoQueryConfig().getTemporality() == HistoQueryConfig.TEMPORALITY.CUMULATIVE) {
      var largestSampleSize = readings.stream().map(r -> r.count).max(Long::compare).orElse(0L);
      var largestSample =
          readings.stream().filter(f -> Objects.equals(f.count, largestSampleSize)).findFirst();
      if (largestSample.isPresent()) {
        var histo = getHistogramResponse(largestSample.get());
        return GetMetricsResponse.builder()
            .resource(query.getSvc())
            .metric(query.getMetric())
            .tags(query.getTags())
            .histogramResponse(histo)
            .build();
      } else {
        return CannedResponses.noMetricsResponse(
            query.getSvc(), query.getMetric(), query.getTags());
      }
    } else if (query.getHistoQueryConfig().getTemporality() == HistoQueryConfig.TEMPORALITY.DELTA
        || query.getHistoQueryConfig().getTemporality() == HistoQueryConfig.TEMPORALITY.MERGED) {
      var deltas =
          readings.stream()
              .filter(sample -> sample.histoType == ChHistoSample.HISTO_TYPE.DELTA)
              .toList();
      var merged =
          TwoAtATimeMerger.merge(
              deltas,
              (a, b) -> {
                var distA = new HistogramMerger.Distribution(a.getBuckets(), a.getCounts());
                var disB = new HistogramMerger.Distribution(b.getBuckets(), b.getCounts());
                var resampled = HistogramMerger.merge(distA, disB);
                var sum = a.getSum() + b.getSum();
                var count = a.getCount() + b.getCount();
                var tsStart = Math.min(a.tsStart, b.tsStart);
                var tsEnd = Math.max(a.tsEnd, b.tsEnd);
                var min = Math.min(a.min, b.min);
                var max = Math.max(a.max, b.max);
                return ChHistoSample.builder()
                    .resource(query.getSvc())
                    .metric(query.getMetric())
                    .tags(query.getTags())
                    .histoType(histoType)
                    .sum(sum)
                    .min(min)
                    .max(max)
                    .count(count)
                    .tsStart(tsStart)
                    .tsEnd(tsEnd)
                    .counts(resampled.counts())
                    .buckets(resampled.buckets())
                    .build();
              });
      if (merged.isPresent()) {
        var histo = getHistogramResponse(merged.get());
        return GetMetricsResponse.builder()
            .resource(query.getSvc())
            .metric(query.getMetric())
            .tags(query.getTags())
            .histogramResponse(histo)
            .build();
      } else {
        return CannedResponses.noMetricsResponse(
            query.getSvc(), query.getMetric(), query.getTags());
      }
    }
    return CannedResponses.noMetricsResponse(query.getSvc(), query.getMetric(), query.getTags());
  }

  public GetHistogramResponse getHistogramResponse(ChHistoSample chHistoSample) {
    var histogram =
        Histogram.builder()
            .start(chHistoSample.getTsStart())
            .end(chHistoSample.getTsEnd())
            .count(chHistoSample.getCount())
            .sum(chHistoSample.getSum())
            .counts(chHistoSample.getCounts() == null ? List.of() : Ints.asList(chHistoSample.getCounts()))
            .buckets(
                chHistoSample.getBuckets() == null
                    ? List.of()
                    : Floats.asList(chHistoSample.getBuckets()))
            .build();
    return GetHistogramResponse.builder().histograms(List.of(histogram)).build();
  }

  public List<ChHistoSample> scanSamples(
      long ts,
      long te,
      String resource,
      String metrics,
      Map<String, String> tags,
      ChHistoSample.HISTO_TYPE histoType) {
    var query =
        renderHistoQuery(
            ChGetHistoQueryTemplate.builder()
                .table(ChConstants.TBL_HISTOS)
                .resource(resource)
                .metric(metrics)
                .tags(tags)
                .histoType(histoType.name())
                .ts(ts)
                .te(te)
                .build());

    List<GenericRecord> records = client.queryAll(query);
    var samples = new ArrayList<ChHistoSample>(records.size());
    for (var record : records) {
      float[] buckets;
      int[] counts;
      try {
        buckets = record.getFloatArray("buckets");
      } catch (Exception e) {
        var list = record.getList("buckets");
        if (list == null) {
          buckets = null;
        } else {
          buckets = new float[list.size()];
          for (int i = 0; i < list.size(); i++) {
            buckets[i] = ((Number) list.get(i)).floatValue();
          }
        }
      }
      try {
        counts = record.getIntArray("counts");
      } catch (Exception e) {
        var list = record.getList("counts");
        counts = list == null ? null : list.stream().mapToInt(o -> ((Number) o).intValue()).toArray();
      }
      long totalCount = 0L;
      if (counts != null) {
        for (int c : counts) {
          totalCount += c;
        }
      }

      float min = 0f;
      float max = 0f;
      if (buckets != null && buckets.length > 0) {
        min = buckets[0];
        max = buckets[buckets.length - 1];
      }

      @SuppressWarnings("unchecked")
      var recordTags = (Map<String, String>) record.getObject("tags");
      var histoTypeStr =
          record.hasValue("histo_type") ? record.getString("histo_type") : histoType.name();

      samples.add(
          ChHistoSample.builder()
              .resource(record.getString("resource"))
              .metric(record.getString("metric_name"))
              .tags(recordTags)
              .tsStart(record.getLong("ts_start_ms"))
              .tsEnd(record.getLong("ts_end_ms"))
              .buckets(buckets)
              .counts(counts)
              .count(totalCount)
              .min(min)
              .max(max)
              .histoType(ChHistoSample.HISTO_TYPE.valueOf(histoTypeStr))
              .build());
    }

    return samples;
  }

  private String renderHistoQuery(ChGetHistoQueryTemplate data) {
    TemplateOutput output = new StringOutput();
    templateEngine.render(ChJteTemplateFiles.GET_HISTO_SAMPLES, data, output);
    return output.toString();
  }
}
