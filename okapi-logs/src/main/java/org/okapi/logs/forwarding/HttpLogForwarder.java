package org.okapi.logs.forwarding;

import io.micrometer.core.instrument.MeterRegistry;
import io.opentelemetry.proto.logs.v1.LogRecord;
import io.opentelemetry.proto.logs.v1.ScopeLogs;
import java.util.List;
import okhttp3.OkHttpClient;
import org.okapi.logs.spring.HttpClientConfiguration;
import org.okapi.swim.ping.Member;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class HttpLogForwarder implements LogForwarder {
  private final MeterRegistry meterRegistry;
  private final OkHttpClient httpClient;

  public HttpLogForwarder(
      @Autowired MeterRegistry meterRegistry,
      @Autowired @Qualifier(HttpClientConfiguration.LOGS_OK_HTTP) OkHttpClient httpClient) {
    this.meterRegistry = meterRegistry;
    this.httpClient = httpClient;
  }

  @Override
  public void forward(String tenantId, String logStream, Member member, List<LogRecord> records) {
    if (member == null || records == null || records.isEmpty()) return;

    var scopeLog = ScopeLogs.newBuilder().addAllLogRecords(records).build();
    byte[] bytes = scopeLog.toByteArray();

    var url = String.format("http://%s:%d/v1/logs/bulk", member.getIp(), member.getPort());
    var body =
        okhttp3.RequestBody.create(bytes, okhttp3.MediaType.parse("application/octet-stream"));

    var request =
        new okhttp3.Request.Builder()
            .url(url)
            .post(body)
            .addHeader("X-Okapi-Tenant-Id", tenantId)
            .addHeader("X-Okapi-Log-Stream", logStream)
            .build();

    meterRegistry.counter("okapi.logs.forward_records_total").increment(records.size());
    try (var response = httpClient.newCall(request).execute()) {
      if (response.isSuccessful()) {
        meterRegistry.counter("okapi.logs.forward_success_total").increment(records.size());
      } else {
        meterRegistry.counter("okapi.logs.forward_failed_total").increment(records.size());
      }
    } catch (Exception e) {
      meterRegistry.counter("okapi.logs.forward_failed_total").increment(records.size());
    }
  }
}
