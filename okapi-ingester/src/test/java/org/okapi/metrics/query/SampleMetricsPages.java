/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.metrics.query;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.okapi.metrics.io.MetricsPage;
import org.okapi.metrics.io.MetricsPageCodec;
import org.okapi.rest.metrics.ExportMetricsRequest;
import org.okapi.rest.metrics.payloads.Gauge;
import org.okapi.s3.ByteArrayByteRangeSupplier;
import org.okapi.s3.ByteRangeSupplier;

public class SampleMetricsPages {
  public static final String METRIC_NAME = "cpu_usage";
  public static final String METRIC_NAME_2 = "cpu_usage_2";
  public static final Map<String, String> METRIC_TAGS =
      new TreeMap<>() {
        {
          put("host", "server1");
          put("region", "us-west");
        }
      };

  // A convenient alternate tag map for mismatch/superset tests
  public static final Map<String, String> METRIC_TAGS_SUPERSET =
      new TreeMap<>() {
        {
          put("host", "server1");
          put("region", "us-west");
          put("env", "prod");
        }
      };

  public static ByteRangeSupplier getRangeSupplier(byte[] data) {
    return new ByteArrayByteRangeSupplier(data);
  }

  public static byte[] getGaugePage() throws IOException {
    var os = new ByteArrayOutputStream();
    os.write(getMetricsGaugePage1());
    os.write(getMetricsGaugePage2());
    return os.toByteArray();
  }

  public static byte[] getPagesWithDifferentMetrics() throws IOException {
    var os = new ByteArrayOutputStream();
    os.write(getMetricsGaugePage1());
    os.write(getMetricsGaugePage3());
    return os.toByteArray();
  }

  public static byte[] getPagesWithMetricsApart() throws IOException {
    var os = new ByteArrayOutputStream();
    os.write(getMetricsGaugePage1());
    os.write(getMetricsGaugePage3());
    os.write(getMetricsGaugePage2());
    return os.toByteArray();
  }

  public static byte[] getMetricsGaugePage1() throws IOException {
    var page = new MetricsPage(1000L, 1000L, 1000, 0.01f);
    var gaugeReq =
        ExportMetricsRequest.builder()
            .metricName(METRIC_NAME)
            .tags(METRIC_TAGS)
            .gauge(
                Gauge.builder()
                    .ts(List.of(1001L, 1002L, 2000L, 4000L))
                    .value(List.of(0.5f, 0.75f, 0.9f, 0.6f))
                    .build())
            .build();
    page.append(gaugeReq);
    var codec = new MetricsPageCodec();
    return codec.serialize(page);
  }

  public static byte[] getMetricsGaugePage2() throws IOException {
    var page = new MetricsPage(1000L, 1000L, 1000, 0.01f);
    var gaugeReq =
        ExportMetricsRequest.builder()
            .metricName(METRIC_NAME)
            .tags(METRIC_TAGS)
            .gauge(
                Gauge.builder()
                    .ts(List.of(3000L, 5000L, 6000L))
                    .value(List.of(0.8f, 0.65f, 0.7f))
                    .build())
            .build();
    page.append(gaugeReq);
    var codec = new MetricsPageCodec();
    return codec.serialize(page);
  }

  public static byte[] getMetricsGaugePage3() throws IOException {
    var page = new MetricsPage(1000L, 1000L, 1000, 0.01f);
    var gaugeReq =
        ExportMetricsRequest.builder()
            .metricName(METRIC_NAME_2)
            .tags(METRIC_TAGS)
            .gauge(Gauge.builder().ts(List.of(7000L, 8000L)).value(List.of(0.55f, 0.45f)).build())
            .build();
    page.append(gaugeReq);
    var codec = new MetricsPageCodec();
    return codec.serialize(page);
  }

  // QoL: Write bytes to a temporary file and return its Path
  public static Path writeToTempFile(byte[] data) throws IOException {
    Path tmp = Files.createTempFile("okapi_metrics_page_", ".bin");
    Files.write(tmp, data);
    tmp.toFile().deleteOnExit();
    return tmp;
  }
}
