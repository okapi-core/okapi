/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.metrics.service;

import com.google.gson.Gson;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import org.okapi.rest.metrics.ForwardedMetricsRequest;
import org.okapi.spring.configs.HttpClientConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

public class MetricsForwarderImpl implements MetricsForwarder {
  OkHttpClient httpClient;
  Gson gson;

  public MetricsForwarderImpl(
      @Autowired @Qualifier(HttpClientConfiguration.LOGS_OK_HTTP) OkHttpClient httpClient) {
    this.httpClient = httpClient;
    this.gson = new Gson();
  }

  @Override
  public void forward(String ip, int port, ForwardedMetricsRequest exportMetricsRequest) {
    var body = gson.toJson(exportMetricsRequest);
    var req =
        new okhttp3.Request.Builder()
            .url("http://" + ip + ":" + port + "/v1/metrics/bulk")
            .post(RequestBody.create(okhttp3.MediaType.parse("application/json"), body))
            .build();
    try (var resp = httpClient.newCall(req).execute()) {
      if (!resp.isSuccessful()) {
        throw new RuntimeException(
            "Failed to forward metrics to " + ip + ":" + port + " - " + resp.code());
      }
    } catch (Exception e) {
      throw new RuntimeException(
          "Failed to forward metrics to " + ip + ":" + port + " - " + e.getMessage(), e);
    }
  }
}
