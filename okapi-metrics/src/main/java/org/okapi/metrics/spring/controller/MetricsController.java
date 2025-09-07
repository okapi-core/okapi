package org.okapi.metrics.spring.controller;

import jakarta.validation.Valid;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.okapi.beans.Configurations;
import org.okapi.exceptions.BadRequestException;
import org.okapi.metrics.OutsideWindowException;
import org.okapi.metrics.SharedMessageBox;
import org.okapi.metrics.service.runnables.MetricsWriter;
import org.okapi.metrics.service.web.QueryProcessor;
import org.okapi.metrics.stats.StatisticsFrozenException;
import org.okapi.rest.metrics.*;
import org.rocksdb.RocksDBException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/metrics")
public class MetricsController {

  @Autowired MetricsWriter metricsWriter;
  @Autowired QueryProcessor queryProcessor;

  @Qualifier(Configurations.BEAN_FDB_MESSAGE_BOX)
  @Autowired
  SharedMessageBox<SubmitMetricsRequestInternal> messageBox;

  @PostMapping("")
  public SubmitMetricsResponse submit(
      @Valid @RequestBody SubmitMetricsRequestInternal submitMetricsBatchRequest)
      throws BadRequestException,
          OutsideWindowException,
          InterruptedException,
          StatisticsFrozenException {
    metricsWriter.onRequestArrive(submitMetricsBatchRequest);
    return new SubmitMetricsResponse("OK");
  }

  @PostMapping("/q")
  public GetMetricsResponse get(@RequestBody @Valid GetMetricsRequestInternal requestV2)
      throws Exception {
    var ans = queryProcessor.getMetricsResponse(requestV2);
    return ans;
  }

  @PostMapping("/s")
  public SearchMetricsResponse search(@RequestBody @Valid SearchMetricsRequestInternal request)
      throws BadRequestException, IOException, RocksDBException {
    return queryProcessor.searchMetricsResponse(request);
  }

  @PostMapping("/list")
  public ListMetricsResponse search(@RequestBody @Valid ListMetricsRequest request)
          throws BadRequestException, IOException, RocksDBException {
    return queryProcessor.listMetricsResponse(request);
  }


  @GetMapping("/lag")
  public int lag() {
    return messageBox.size();
  }

  @GetMapping(path = "/ready")
  public String isReady() {
    var isReady = metricsWriter.isReady();
    if (isReady) {
      return "YES";
    } else {
      return "NO";
    }
  }
}
