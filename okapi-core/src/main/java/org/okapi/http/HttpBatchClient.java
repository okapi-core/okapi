package org.okapi.http;

import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import okhttp3.*;

public class HttpBatchClient {
  private final List<HttpRequestSpec> requests = new ArrayList<>();
  private final OkHttpClient baseClient;
  private Duration timeout = Duration.ofSeconds(3);
  private int retries = 1;
  private ExecutorService executor = Executors.newCachedThreadPool();

  public enum HttpMethod {
    GET, POST, PUT, DELETE, PATCH, HEAD
  }

  private HttpBatchClient(OkHttpClient client) {
    this.baseClient = client;
  }

  public static HttpBatchClient create(OkHttpClient client) {
    return new HttpBatchClient(client);
  }

  public HttpBatchClient add(HttpRequestSpec spec) {
    requests.add(spec);
    return this;
  }

  public HttpBatchClient broadcast(List<String> endpoints, HttpMethod method, String path, Map<String, String> headers, RequestBody body) {
    if (method == HttpMethod.GET && body != null) {
      throw new IllegalArgumentException("GET request must not have a body");
    }

    for (String endpoint : endpoints) {
      String url = endpoint.endsWith("/") ? endpoint + path : endpoint + "/" + path;
      RequestBody finalBody = body != null ? body : RequestBody.create(new byte[0]);
      HttpRequestSpec spec = new HttpRequestSpec(method, url, headers, method == HttpMethod.GET ? null : finalBody);
      add(spec);
    }
    return this;
  }

  public HttpBatchClient withTimeout(Duration timeout) {
    this.timeout = timeout;
    return this;
  }

  public HttpBatchClient withRetries(int retries) {
    this.retries = retries;
    return this;
  }

  public HttpBatchClient withExecutor(ExecutorService executor) {
    this.executor = executor;
    return this;
  }

  public List<Result> executeAll() {
    List<CompletableFuture<Result>> futures = new ArrayList<>();
    for (HttpRequestSpec spec : requests) {
      futures.add(submitWithRetry(spec));
    }
    return futures.stream()
            .map(CompletableFuture::join)
            .toList();
  }

  private CompletableFuture<Result> submitWithRetry(HttpRequestSpec spec) {
    return CompletableFuture.supplyAsync(() -> {
      int attempt = 0;
      while (true) {
        try {
          OkHttpClient clientWithTimeout = baseClient.newBuilder()
                  .callTimeout(timeout)
                  .build();
          Request request = spec.toOkHttpRequest();
          try (Response response = clientWithTimeout.newCall(request).execute()) {
            return Result.ok(new HttpResponse(response));
          }
        } catch (Exception ex) {
          attempt++;
          if (attempt > retries) {
            return Result.error(ex);
          }
          try {
            Thread.sleep(100L * attempt); // Simple linear backoff
          } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
            return Result.error(ex);
          }
        }
      }
    }, executor);
  }

  public record HttpRequestSpec(
          HttpMethod method,
          String url,
          Map<String, String> headers,
          RequestBody body
  ) {
    public Request toOkHttpRequest() {
      Request.Builder builder = new Request.Builder().url(url);
      if (headers != null) {
        headers.forEach(builder::addHeader);
      }
      builder.method(method.name(), body);
      return builder.build();
    }
  }

  public static final class HttpResponse {
    private final int code;
    private final String body;
    private final Headers headers;

    public HttpResponse(Response response) throws IOException {
      this.code = response.code();
      this.body = response.body() != null ? response.body().string() : "";
      this.headers = response.headers();
    }

    public int code() {
      return code;
    }

    public String body() {
      return body;
    }

    public Headers headers() {
      return headers;
    }
  }

  public record Result(HttpResponse response, Exception error) {
    public static Result ok(HttpResponse response) {
      return new Result(response, null);
    }

    public static Result error(Exception error) {
      return new Result(null, error);
    }

    public boolean isOk() {
      return error == null;
    }

    public HttpResponse response() {
      return response;
    }

    public Exception error() {
      return error;
    }
  }
}
