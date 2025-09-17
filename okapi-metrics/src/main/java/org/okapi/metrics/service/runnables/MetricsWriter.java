package org.okapi.metrics.service.runnables;

import org.okapi.rest.metrics.ExportMetricsRequest;
import org.okapi.exceptions.BadRequestException;
import org.okapi.metrics.OutsideWindowException;
import org.okapi.metrics.common.sharding.ShardsAndSeriesAssigner;
import org.okapi.metrics.io.StreamReadingException;
import org.okapi.metrics.stats.StatisticsFrozenException;

import java.io.IOException;

public interface MetricsWriter {
  void onRequestArrive(ExportMetricsRequest request)
      throws BadRequestException,
          OutsideWindowException,
          InterruptedException,
          StatisticsFrozenException;

  void setShardsAndSeriesAssigner(ShardsAndSeriesAssigner shardsAndSeriesAssigner);

  boolean isReady();

  void init() throws IOException, StreamReadingException;
}
