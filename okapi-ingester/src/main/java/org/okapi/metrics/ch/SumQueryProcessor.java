package org.okapi.metrics.ch;

import com.clickhouse.client.api.Client;
import com.clickhouse.client.api.query.GenericRecord;
import gg.jte.TemplateOutput;
import gg.jte.output.StringOutput;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.okapi.ch.ChJteTemplateFiles;
import org.okapi.metrics.ch.template.ChMetricTemplateEngine;
import org.okapi.rest.metrics.query.CannedResponses;
import org.okapi.rest.metrics.query.GetMetricsRequest;
import org.okapi.rest.metrics.query.GetMetricsResponse;
import org.okapi.rest.metrics.query.GetSumsQueryConfig;
import org.okapi.rest.metrics.query.GetSumsResponse;
import org.okapi.rest.metrics.query.Sum;
import org.springframework.stereotype.Service;

/** Handles sum query execution and aggregation. */
@Service
public class SumQueryProcessor {
  private final Client client;
  private final ChMetricTemplateEngine templateEngine;

  public SumQueryProcessor(Client client, ChMetricTemplateEngine templateEngine) {
    this.client = client;
    this.templateEngine = templateEngine;
  }

  public GetMetricsResponse getSumRes(GetMetricsRequest query) {
    var resource = query.getSvc();
    var metric = query.getMetric();
    var tags = query.getTags();
    var ts = query.getStart();
    var te = query.getEnd();
    var temporality =
        query.getSumsQueryConfig() == null
            ? GetSumsQueryConfig.TEMPORALITY.DELTA_AGGREGATE
            : query.getSumsQueryConfig().getTemporality();
    var sumType =
        switch (temporality) {
          case CUMULATIVE -> CH_SUM_TYPE.CUMULATIVE;
          case DELTA_AGGREGATE -> CH_SUM_TYPE.DELTA;
        };
    var samples = scanSumSamples(ts, te, resource, metric, tags, sumType);
    if (samples.isEmpty()) {
      return CannedResponses.noMetricsResponse(resource, metric, tags);
    }

    List<Sum> sums;
    switch (temporality) {
      case CUMULATIVE -> {
        var maxSample =
            samples.stream().max(Comparator.comparingLong(ChSumSample::value)).orElse(null);
        if (maxSample == null) {
          return CannedResponses.noMetricsResponse(resource, metric, tags);
        }
        sums =
            List.of(
                Sum.builder()
                    .ts(maxSample.tsStart())
                    .te(maxSample.tsEnd())
                    .count(maxSample.value())
                    .build());
      }
      case DELTA_AGGREGATE -> {
        long total =
            samples.stream()
                .filter(s -> s.sumType() == CH_SUM_TYPE.DELTA)
                .mapToLong(ChSumSample::value)
                .sum();
        long aggTsStart = samples.stream().mapToLong(ChSumSample::tsStart).min().orElse(ts);
        long aggTsEnd = samples.stream().mapToLong(ChSumSample::tsEnd).max().orElse(te);
        sums = List.of(Sum.builder().ts(aggTsStart).te(aggTsEnd).count(total).build());
      }
      default -> {
        sums =
            samples.stream()
                .filter(s -> s.sumType() == CH_SUM_TYPE.DELTA)
                .map(s -> Sum.builder().ts(s.tsStart()).te(s.tsEnd()).count(s.value()).build())
                .toList();
      }
    }

    return GetMetricsResponse.builder()
        .resource(resource)
        .metric(metric)
        .tags(tags)
        .sumsResponse(GetSumsResponse.builder().sums(sums).build())
        .build();
  }

  public String chScanSumsQuery(
      long ts,
      long te,
      String resource,
      String metric,
      Map<String, String> tags,
      CH_SUM_TYPE sumType) {

    var template =
        ChGetSumQueryTemplate.builder()
            .table(ChConstants.TBL_SUM)
            .resource(resource)
            .metric(metric)
            .tags(tags)
            .ts(ts)
            .te(te)
            .histoType(sumType.name())
            .build();
    TemplateOutput output = new StringOutput();
    templateEngine.render(ChJteTemplateFiles.GET_SUM_SAMPLES, template, output);
    return output.toString();
  }

  private List<ChSumSample> scanSumSamples(
      long ts,
      long te,
      String resource,
      String metric,
      Map<String, String> tags,
      CH_SUM_TYPE sumType) {

    var query = chScanSumsQuery(ts, te, resource, metric, tags, sumType);
    List<GenericRecord> records = client.queryAll(query);
    var samples = new ArrayList<ChSumSample>(records.size());
    for (var record : records) {
      @SuppressWarnings("unchecked")
      var recordTags = (Map<String, String>) record.getObject("tags");
      samples.add(
          new ChSumSample(
              record.getLong("ts_start_ms"),
              record.getLong("ts_end_ms"),
              record.getLong("value"),
              CH_SUM_TYPE.valueOf(record.getString("histo_type")),
              recordTags));
    }
    return samples;
  }
}
