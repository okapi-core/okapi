package org.okapi.metrics.ch;

import com.google.gson.Gson;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.okapi.comparables.ComparableMath;
import org.okapi.rest.metrics.ExportMetricsRequest;
import org.okapi.rest.metrics.query.METRIC_TYPE;
import org.okapi.wal.frame.WalEntry;
import org.okapi.wal.io.WalReader;
import org.okapi.wal.manager.WalManager;

@Slf4j
@RequiredArgsConstructor
public class ChMetricsWalConsumer {
  final WalReader walReader;
  final int batchSize;
  final ChWriter chWriter;
  final WalManager walManager;
  Gson gson = new Gson();

  public ChMetricsWalConsumer(int batchSize, ChWriter chWriter, ChWalResources resources) {
    this.walReader = resources.getReader();
    this.batchSize = batchSize;
    this.chWriter = chWriter;
    this.walManager = resources.getManager();
  }

  public void consumeRecords() throws IOException, InterruptedException, ExecutionException {
    var batch = walReader.readBatchAndAdvance(batchSize);
    List<ChGaugeSampleRow> gaugeSamples = new ArrayList<>();
    List<ChHistoSample> histoSamples = new ArrayList<>();
    List<ChSumSampleRow> sumSamples = new ArrayList<>();
    List<String> metaRows = new ArrayList<>();
    for (var entry : batch) {
      var req = gson.fromJson(new String(entry.getPayload()), ExportMetricsRequest.class);
      if (req.getGauge() != null) {
        for (int i = 0; i < req.getGauge().getTs().size(); i++) {
          var ts = req.getGauge().getTs().get(i);
          gaugeSamples.add(
              ChGaugeSampleRow.builder()
                  .resource(req.getResource() == null ? "" : req.getResource())
                  .metric(req.getMetricName())
                  .tags(req.getTags())
                  .timestamp(ts)
                  .value(req.getGauge().getValue().get(i))
                  .build());
          var map = new java.util.HashMap<String, Object>();
          map.put("event_type", METRIC_TYPE.GAUGE.name());
          map.put("svc", req.getResource() == null ? "" : req.getResource());
          map.put("metric", req.getMetricName());
          map.put("tags", req.getTags());
          map.put("ts_start", ts);
          map.put("ts_end", ts);
          metaRows.add(gson.toJson(map));
        }
      }
      if (req.getHisto() != null) {
        for (int i = 0; i < req.getHisto().getHistoPoints().size(); i++) {
          var pt = req.getHisto().getHistoPoints().get(i);
          var buckets = pt.getBuckets();
          var counts = pt.getBucketCounts();
          ChHistoSample.HISTO_TYPE histoType =
              switch (pt.getTemporality()) {
                case DELTA -> ChHistoSample.HISTO_TYPE.DELTA;
                case CUMULATIVE -> ChHistoSample.HISTO_TYPE.CUMULATIVE;
              };
          // compute min/max from buckets when available
          float min = 0f;
          float max = 0f;
          if (buckets != null && buckets.length > 0) {
            min = buckets[0];
            max = buckets[buckets.length - 1];
          }
          histoSamples.add(
              ChHistoSample.builder()
                  .resource(req.getResource())
                  .histoType(histoType)
                  .metric(req.getMetricName())
                  .tags(req.getTags())
                  .tsStart(pt.getStart())
                  .tsEnd(pt.getEnd())
                  .min(min)
                  .max(max)
                  .buckets(buckets)
                  .counts(counts)
                  .build());
          var map = new java.util.HashMap<String, Object>();
          map.put("event_type", METRIC_TYPE.HISTO.name());
          map.put("svc", req.getResource() == null ? "" : req.getResource());
          map.put("metric", req.getMetricName());
          map.put("tags", req.getTags());
          map.put("ts_start", pt.getStart());
          map.put("ts_end", pt.getEnd());
          metaRows.add(gson.toJson(map));
        }
      }

      if (req.getSum() != null) {
        for (int i = 0; i < req.getSum().getSumPoints().size(); i++) {
          var pt = req.getSum().getSumPoints().get(i);
          CH_SUM_TYPE sumType =
              switch (req.getSum().getTemporality()) {
                case DELTA -> CH_SUM_TYPE.DELTA;
                case CUMULATIVE -> CH_SUM_TYPE.CUMULATIVE;
              };
          sumSamples.add(
              ChSumSampleRow.builder()
                  .resource(req.getResource())
                  .metricName(req.getMetricName())
                  .tags(req.getTags())
                  .tsStart(pt.getStart())
                  .tsEnd(pt.getEnd())
                  .value(pt.getSum())
                  .sumType(sumType)
                  .build());
          var map = new java.util.HashMap<String, Object>();
          map.put("event_type", METRIC_TYPE.SUM.name());
          map.put("svc", req.getResource() == null ? "" : req.getResource());
          map.put("metric", req.getMetricName());
          map.put("tags", req.getTags());
          map.put("ts_start", pt.getStart());
          map.put("ts_end", pt.getEnd());
          metaRows.add(gson.toJson(map));
        }
      }
    }

    var gaugeRows = gaugeSamples.stream().map(gson::toJson).toList();
    var histoRows =
        histoSamples.stream()
            .map(
                h -> {
                  var map = new java.util.HashMap<String, Object>();
                  map.put("resource", h.getResource());
                  map.put("metric_name", h.getMetric());
                  map.put("tags", h.getTags());
                  map.put("ts_start", h.getTsStart());
                  map.put("ts_end", h.getTsEnd());
                  map.put("buckets", h.getBuckets());
                  map.put("counts", h.getCounts());
                  map.put("histo_type", h.getHistoType().name());
                  map.put("min", h.getMin());
                  map.put("max", h.getMax());
                  map.put("sum", h.getSum());
                  map.put("count", h.getCount());
                  return gson.toJson(map);
                })
            .toList();
    var sumRows =
        sumSamples.stream()
            .map(
                s -> {
                  var map = new HashMap<String, Object>();
                  map.put("resource", s.getResource());
                  map.put("metric_name", s.getMetricName());
                  map.put("tags", s.getTags());
                  map.put("ts_start", s.getTsStart());
                  map.put("ts_end", s.getTsEnd());
                  map.put("value", s.getValue());
                  map.put("histo_type", s.getSumType().name());
                  return gson.toJson(map);
                })
            .toList();
    var gaugeInsertRes = chWriter.writeRows(ChConstants.TBL_GAUGES, gaugeRows);
    var histoInsertRes = chWriter.writeRows(ChConstants.TBL_HISTOS, histoRows);
    var sumInsertRes = chWriter.writeRows(ChConstants.TBL_SUM, sumRows);
    var metaInsertRes = chWriter.writeRows(ChConstants.TBL_METRIC_EVENTS_META, metaRows);
    gaugeInsertRes.get();
    histoInsertRes.get();
    sumInsertRes.get();
    metaInsertRes.get();
    var largestLsn = batch.stream().map(WalEntry::getLsn).reduce(ComparableMath::max);
    if (largestLsn.isPresent()) {
      walManager.commitLsn(largestLsn.get());
    }
  }
}
