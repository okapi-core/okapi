package org.okapi.metrics.query;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import java.util.HashMap;
import java.util.Map;
import org.okapi.metrics.config.MetricsCfg;
import org.okapi.rest.metrics.ExportMetricsRequest;
import org.okapi.rest.metrics.payloads.Gauge;
import org.okapi.rest.metrics.payloads.Histo;
import org.okapi.rest.metrics.payloads.HistoPoint;
import org.springframework.beans.factory.annotation.Autowired;

public class PayloadSplitter {

  long blockDurationMs;

  public PayloadSplitter(@Autowired MetricsCfg cfg) {
    this.blockDurationMs = cfg.getIdxExpiryDuration();
  }

  public Multimap<Long, ExportMetricsRequest> splitPayloadByBlocks(ExportMetricsRequest request) {

    var requests = ArrayListMultimap.<Long, ExportMetricsRequest>create();
    if (request.getGauge() != null) {
      var split = splitGaugeByBlocks(request.getGauge());
      for (var entry : split.entrySet()) {
        var newReq =
            ExportMetricsRequest.builder()
                .metricName(request.getMetricName())
                .tags(request.getTags())
                .gauge(entry.getValue())
                .build();
        requests.put(entry.getKey(), newReq);
      }
    }
    if (request.getHisto() != null) {
      var split = splitHistoByBlocks(request.getHisto());
      for (var entry : split.asMap().entrySet()) {
        var newReq =
            ExportMetricsRequest.builder()
                .metricName(request.getMetricName())
                .tags(request.getTags())
                .histo(Histo.builder().histoPoints(entry.getValue().stream().toList()).build())
                .build();
        requests.put(entry.getKey(), newReq);
      }
    }
    return requests;
  }

  public Multimap<Long, HistoPoint> splitHistoByBlocks(Histo histo) {
    // treat it the same as a gauge which starts at start time
    var multimap = ArrayListMultimap.<Long, HistoPoint>create();
    for (var pt : histo.getHistoPoints()) {
      var block = pt.getStart() / blockDurationMs;
      multimap.put(block, pt);
    }
    return multimap;
  }

  public Map<Long, Gauge> splitGaugeByBlocks(Gauge gauge) {
    var gauges = new HashMap<Long, Gauge>();
    if (gauge.getTs().isEmpty()) {
      return gauges;
    }
    var st = 0;
    var curBlock = gauge.getTs().getFirst() / blockDurationMs;
    for (int i = 1; i < gauge.getTs().size(); i++) {
      var block = (int) (gauge.getTs().get(i) / blockDurationMs);
      if (block != curBlock) {
        // create a new gauge for the previous block
        var newGauge =
            Gauge.builder()
                .ts(gauge.getTs().subList(st, i))
                .value(gauge.getValue().subList(st, i))
                .build();
        gauges.put(curBlock, newGauge);
        st = i;
        curBlock = block;
      }
    }
    if (st < gauge.getTs().size()) {
      var newGauge =
          Gauge.builder()
              .ts(gauge.getTs().subList(st, gauge.getTs().size()))
              .value(gauge.getValue().subList(st, gauge.getValue().size()))
              .build();
      gauges.put(curBlock, newGauge);
    }
    return gauges;
  }
}
