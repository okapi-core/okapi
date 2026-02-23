/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.traces.service;

import com.google.gson.Gson;
import io.micrometer.core.instrument.MeterRegistry;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.okapi.identity.Member;
import org.okapi.spring.configs.HttpClientConfiguration;
import org.okapi.traces.io.ForwardedSpanRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

@Slf4j
public class HttpTraceForwarder {
  private final OkHttpClient httpClient;
  private final MeterRegistry meterRegistry;
  private final Gson gson = new Gson();

  public HttpTraceForwarder(
      @Autowired MeterRegistry meterRegistry,
      @Autowired @Qualifier(HttpClientConfiguration.LOGS_OK_HTTP) OkHttpClient httpClient) {
    this.httpClient = httpClient;
    this.meterRegistry = meterRegistry;
  }

  public void forward(Member member, ForwardedSpanRecord forwardedSpanRecord) {
    var ip = member.getIp();
    var port = member.getPort();
    var url = "http://" + ip + ":" + port + "/v1/traces/bulk";
    var body = gson.toJson(forwardedSpanRecord).getBytes();
    var req = new Request.Builder().url(url).post(RequestBody.create(body)).build();

    var count = forwardedSpanRecord.getRecords().size();
    try (var res = httpClient.newCall(req).execute()) {
      if (res.isSuccessful()) {
        meterRegistry.counter("okapi.traces.forward_success_total").increment(count);
      } else {
        meterRegistry.counter("okapi.logs.forward_failed_total").increment(count);
      }
    } catch (IOException e) {
      meterRegistry.counter("okapi.logs.forward_failed_total").increment(count);
    }
  }
}
