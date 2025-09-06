package org.okapi.metrics.service.runnables;

import static org.okapi.validation.OkapiChecks.checkArgument;

import com.apple.foundationdb.Database;
import java.io.IOException;
import org.okapi.exceptions.BadRequestException;
import org.okapi.metrics.SharedMessageBox;
import org.okapi.metrics.common.sharding.ShardsAndSeriesAssigner;
import org.okapi.metrics.io.StreamReadingException;
import org.okapi.rest.metrics.SubmitMetricsRequestInternal;

public class FdbMetricsWriter implements MetricsWriter {
  public static final String FDB = FdbMetricsWriter.class.getSimpleName();
  Database db;
  String self;
  SharedMessageBox<SubmitMetricsRequestInternal> messageBox;
  public static final Long MAX_LENGTH = 1000L;

  public FdbMetricsWriter(
      Database fdb, String self, SharedMessageBox<SubmitMetricsRequestInternal> messageBox) {
    this.db = fdb;
    this.self = self;
    this.messageBox = messageBox;
  }

  @Override
  public void onRequestArrive(SubmitMetricsRequestInternal request)
      throws BadRequestException, InterruptedException {
    checkArgument(
        request.getTs().length == request.getValues().length,
        () -> {
          var msg =
              String.format(
                  "Malformed request, there are %d timestamps and %d values, they should be the same.",
                  request.getTs().length, request.getValues().length);
          return new BadRequestException(msg);
        });
    checkArgument(
        request.getTs().length < MAX_LENGTH,
        () -> {
          var msg = String.format("Maximum number of data points per request is %d but got %d", MAX_LENGTH, request.getValues().length);
          return new BadRequestException(msg);
        });
    messageBox.push(request);
  }

  @Override
  public void setShardsAndSeriesAssigner(ShardsAndSeriesAssigner shardsAndSeriesAssigner) {
    // no concept of shards or series
    return;
  }

  @Override
  public boolean isReady() {
    return true;
  }

  @Override
  public void init() throws IOException, StreamReadingException {}
}
