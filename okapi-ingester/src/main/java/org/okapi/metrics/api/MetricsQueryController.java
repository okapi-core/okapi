/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.metrics.api;

import lombok.AllArgsConstructor;
import org.okapi.logs.query.QueryConfig;
import org.okapi.metrics.query.MultisourceMetricsQueryService;
import org.okapi.metrics.query.OnDiskMetricsQueryProcessor;
import org.okapi.metrics.query.rest.GaugeScanQuery;
import org.okapi.metrics.query.rest.HistogramScanQuery;
import org.okapi.primitives.ReadonlyHistogram;
import org.okapi.primitives.TimestampedReadonlySketch;
import org.okapi.rest.metrics.query.GetMetricsRequest;
import org.okapi.rest.metrics.query.GetMetricsResponse;
import org.okapi.spring.configs.Profiles;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
@Profile(Profiles.PROFILE_OKAPI_ENGINE)
public class MetricsQueryController {

  OnDiskMetricsQueryProcessor onDiskMetricsQueryProcessor;
  MultisourceMetricsQueryService multisourceMetricsQueryService;

  @PostMapping("/metrics/query/gaugeSketches")
  public TimestampedReadonlySketch[] getGaugeSketches(@RequestBody GaugeScanQuery scanQuery)
      throws Exception {
    return onDiskMetricsQueryProcessor
        .getGaugeSketches(
            scanQuery.getName(),
            scanQuery.getTags(),
            scanQuery.getResType(),
            scanQuery.getStart(),
            scanQuery.getEnd())
        .toArray(new TimestampedReadonlySketch[0]);
  }

  @PostMapping("/metrics/query/histoSketches")
  public ReadonlyHistogram[] getHistogram(@RequestBody HistogramScanQuery scanQuery)
      throws Exception {
    return onDiskMetricsQueryProcessor
        .getHistograms(
            scanQuery.getName(), scanQuery.getTags(), scanQuery.getStart(), scanQuery.getEnd())
        .toArray(new ReadonlyHistogram[0]);
  }

  @PostMapping("/metrics/query")
  public GetMetricsResponse getMetricsResponse(@RequestBody GetMetricsRequest getMetricsRequest)
      throws Exception {
    var response =
        multisourceMetricsQueryService.getMetricsResponse(
            getMetricsRequest, QueryConfig.allSources());
    return response;
  }

  @PostMapping("/metrics/query/local")
  public GetMetricsResponse getMetricsResponseLocal(
      @RequestBody GetMetricsRequest getMetricsRequest) throws Exception {
    var response =
        multisourceMetricsQueryService.getMetricsResponse(
            getMetricsRequest, QueryConfig.localSources());
    return response;
  }
}
