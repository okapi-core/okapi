package org.okapi.web.spring.config;

import okhttp3.OkHttpClient;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OkHttpConfig {
  public OkHttpClient getOkHttpClient() {
    return new OkHttpClient();
  }
}
