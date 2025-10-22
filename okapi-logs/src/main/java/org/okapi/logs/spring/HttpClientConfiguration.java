package org.okapi.logs.spring;

import okhttp3.OkHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HttpClientConfiguration {
  public static final String LOGS_OK_HTTP = "logsOkHttp";

  @Bean(name = LOGS_OK_HTTP)
  public OkHttpClient okHttpClient() {
    return new OkHttpClient();
  }
}
