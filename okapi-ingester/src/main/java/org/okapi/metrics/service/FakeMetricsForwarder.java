package org.okapi.metrics.service;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import org.okapi.rest.metrics.ForwardedMetricsRequest;

public class FakeMetricsForwarder implements MetricsForwarder {
  public record Args(String ip, int port, ForwardedMetricsRequest request) {}

  @Getter List<Args> args = new ArrayList<>();

  @Override
  public void forward(String ip, int port, ForwardedMetricsRequest exportMetricsRequest) {
    args.add(new Args(ip, port, exportMetricsRequest));
  }
}
