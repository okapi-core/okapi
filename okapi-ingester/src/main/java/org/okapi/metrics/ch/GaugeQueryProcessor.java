/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.metrics.ch;

import static org.okapi.validation.OkapiChecks.checkArgument;

import com.clickhouse.client.api.Client;
import com.clickhouse.client.api.query.GenericRecord;
import gg.jte.TemplateOutput;
import gg.jte.output.StringOutput;
import java.util.ArrayList;
import java.util.List;
import org.okapi.ch.ChJteTemplateFiles;
import org.okapi.exceptions.BadRequestException;
import org.okapi.metrics.ch.template.ChGetGaugeQueryTemplate;
import org.okapi.metrics.ch.template.ChMetricTemplateEngine;
import org.okapi.metrics.pojos.AGG_TYPE;
import org.okapi.metrics.pojos.RES_TYPE;
import org.okapi.rest.metrics.query.GetGaugeResponse;
import org.okapi.rest.metrics.query.GetMetricsRequest;
import org.okapi.rest.metrics.query.GetMetricsResponse;

/** Handles gauge query execution against ClickHouse. */
public class GaugeQueryProcessor {
  private final Client client;
  private final ChMetricTemplateEngine templateEngine;

  public GaugeQueryProcessor(Client client, ChMetricTemplateEngine templateEngine) {
    this.client = client;
    this.templateEngine = templateEngine;
  }

  public GetMetricsResponse getGaugeRes(GetMetricsRequest getMetricsRequest) {
    checkArgument(
        getMetricsRequest.getGaugeQueryConfig() != null,
        () -> new BadRequestException("gaugeQueryConfig is required for GAUGE"));
    var gaugeQueryConfig = getMetricsRequest.getGaugeQueryConfig();

    checkArgument(
        getMetricsRequest.getGaugeQueryConfig().getAggregation() != null,
        () -> new BadRequestException("An aggregation is required for gauge queries."));
    checkArgument(
        getMetricsRequest.getGaugeQueryConfig().getResolution() != null,
        () -> new BadRequestException("A resolution is required for gauge queries."));
    var aggType = gaugeQueryConfig.getAggregation();
    var resType = gaugeQueryConfig.getResolution();

    var query =
        renderGaugeQuery(
            ChGetGaugeQueryTemplate.builder()
                .table(ChConstants.TBL_GAUGES)
                .bucketExpr(bucketExpr(resType))
                .aggExpr(aggExpr(aggType))
                .resource(getMetricsRequest.getSvc())
                .metric(getMetricsRequest.getMetric())
                .startMs(getMetricsRequest.getStart())
                .endMs(getMetricsRequest.getEnd())
                .tags(getMetricsRequest.getTags())
                .build());

    List<GenericRecord> records = client.queryAll(query);
    var times = new ArrayList<Long>(records.size());
    var values = new ArrayList<Float>(records.size());
    for (var record : records) {
      times.add(record.getLong("bucket_ms"));
      values.add((float) record.getDouble("value"));
    }

    var gaugeResponse =
        GetGaugeResponse.builder()
            .resolution(resType)
            .aggregation(aggType)
            .times(times)
            .values(values)
            .build();

    return GetMetricsResponse.builder()
        .metric(getMetricsRequest.getMetric())
        .tags(getMetricsRequest.getTags())
        .gaugeResponse(gaugeResponse)
        .build();
  }

  private String renderGaugeQuery(ChGetGaugeQueryTemplate templateData) {
    TemplateOutput output = new StringOutput();
    templateEngine.render(ChJteTemplateFiles.GET_GAUGE_SAMPLES, templateData, output);
    return output.toString();
  }

  private static String bucketExpr(RES_TYPE resType) {
    return switch (resType) {
      case SECONDLY -> "toUnixTimestamp64Milli(toStartOfSecond(timestamp))";
      case MINUTELY -> "toUnixTimestamp(toStartOfMinute(timestamp)) * 1000";
      case HOURLY -> "toUnixTimestamp(toStartOfHour(timestamp)) * 1000";
    };
  }

  private static String aggExpr(AGG_TYPE aggType) {
    return switch (aggType) {
      case AVG -> "avg(value)";
      case SUM -> "sum(value)";
      case MIN -> "min(value)";
      case MAX -> "max(value)";
      case COUNT -> "count()";
      case P50 -> "quantile(0.5)(value)";
      case P75 -> "quantile(0.75)(value)";
      case P90 -> "quantile(0.9)(value)";
      case P95 -> "quantile(0.95)(value)";
      case P99 -> "quantile(0.99)(value)";
    };
  }
}
