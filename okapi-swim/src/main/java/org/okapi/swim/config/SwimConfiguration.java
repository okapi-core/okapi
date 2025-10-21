package org.okapi.swim.config;

import com.google.gson.Gson;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lombok.RequiredArgsConstructor;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.okapi.swim.membership.EventDeduper;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@EnableConfigurationProperties(SwimConfig.class)
@RequiredArgsConstructor
public class SwimConfiguration {

  @Bean
  public OkHttpClient okHttpClient(SwimConfig swimConfig) {
    var timeout =
        Duration.ofMillis(swimConfig.getTimeoutMillis() > 0 ? swimConfig.getTimeoutMillis() : 5000);
    var builder =
        new OkHttpClient.Builder()
            .connectTimeout(timeout)
            .readTimeout(timeout)
            .writeTimeout(timeout)
            .callTimeout(timeout)
            .retryOnConnectionFailure(true);

    int retries = Math.max(0, swimConfig.getRetries());
    if (retries > 0) {
      builder.addInterceptor(new RetryInterceptor(retries));
    }
    return builder.build();
  }

  @Bean
  public Gson gson() {
    return new Gson();
  }

  @Bean
  public ExecutorService executorService(SwimConfig swimConfig) {
    int size = swimConfig.getThreadPoolSize() > 0 ? swimConfig.getThreadPoolSize() : 4;
    return Executors.newFixedThreadPool(size);
  }

  @Bean
  public EventDeduper eventDeduper(SwimConfig swimConfig) {
    long ttl = swimConfig.getDedupeTtlMillis() > 0 ? swimConfig.getDedupeTtlMillis() : 60000;
    int maxEntries = swimConfig.getDedupeMaxEntries() > 0 ? swimConfig.getDedupeMaxEntries() : 10000;
    return new EventDeduper(ttl, maxEntries);
  }

  static class RetryInterceptor implements Interceptor {
    private final int maxRetries;

    RetryInterceptor(int maxRetries) {
      this.maxRetries = maxRetries;
    }

    @Override
    public Response intercept(Chain chain) throws java.io.IOException {
      Request request = chain.request();
      java.io.IOException lastException = null;
      for (int attempt = 0; attempt <= maxRetries; attempt++) {
        try {
          Response response = chain.proceed(request);
          // retry on 5xx
          if (response.code() >= 500 && attempt < maxRetries) {
            response.close();
            continue;
          }
          return response;
        } catch (java.io.IOException e) {
          lastException = e;
          if (attempt == maxRetries) {
            throw e;
          }
        }
      }
      // Should not reach here; fallback
      throw lastException != null
          ? lastException
          : new java.io.IOException("Unknown retry failure");
    }
  }
}
