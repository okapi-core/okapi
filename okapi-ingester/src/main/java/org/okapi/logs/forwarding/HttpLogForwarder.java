package org.okapi.logs.forwarding;

import com.google.gson.Gson;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import org.okapi.identity.Member;
import org.okapi.logs.io.ForwardedLogIngestRecord;
import org.okapi.spring.configs.HttpClientConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class HttpLogForwarder implements LogForwarder {
  private final MeterRegistry meterRegistry;
  private final OkHttpClient httpClient;
  private final Gson gson = new Gson();

  public HttpLogForwarder(
      @Autowired MeterRegistry meterRegistry,
      @Autowired @Qualifier(HttpClientConfiguration.LOGS_OK_HTTP) OkHttpClient httpClient) {
    this.meterRegistry = meterRegistry;
    this.httpClient = httpClient;
  }

  @Override
  public void forward(Member member, ForwardedLogIngestRecord forwardedLogIngestRecord) {

    var url = String.format("http://%s:%d/v1/logs/bulk", member.getIp(), member.getPort());
    var bytes = gson.toJson(forwardedLogIngestRecord);
    var body = RequestBody.create(bytes, okhttp3.MediaType.parse("application/json"));

    var request = new okhttp3.Request.Builder().url(url).post(body).build();

    var count = forwardedLogIngestRecord.getRecords().size();
    try (var response = httpClient.newCall(request).execute()) {
      if (response.isSuccessful()) {
        meterRegistry.counter("okapi.logs.forward_success_total").increment(count);
      } else {
        meterRegistry.counter("okapi.logs.forward_failed_total").increment(count);
      }
    } catch (Exception e) {
      meterRegistry.counter("okapi.logs.forward_failed_total").increment(count);
    }
  }
}
