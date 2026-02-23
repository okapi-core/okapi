package org.okapi.nodes;

import java.io.IOException;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

@Slf4j
public class Ec2IpSupplier implements IpSupplier {

  private static final String TOKEN_URL = "http://169.254.169.254/latest/api/token";
  private static final String METADATA_URL = "http://169.254.169.254/latest/meta-data/local-ipv4";
  private static final OkHttpClient client = new OkHttpClient();

  public Ec2IpSupplier() {}

  private static String fetchIMDSToken() throws IOException {
    Request request =
        new Request.Builder()
            .url(TOKEN_URL)
            .put(RequestBody.create(new byte[0]))
            .addHeader("X-aws-ec2-metadata-token-ttl-seconds", "21600")
            .build();

    try (Response response = client.newCall(request).execute()) {
      if (!response.isSuccessful()) {
        throw new IOException("Failed to fetch metadata token: " + response.code());
      }
      return response.body().string();
    }
  }

  private static String fetchPrivateIp(String token) throws IOException {
    Request request =
        new Request.Builder()
            .url(METADATA_URL)
            .addHeader("X-aws-ec2-metadata-token", token)
            .build();

    try (Response response = client.newCall(request).execute()) {
      if (!response.isSuccessful()) {
        throw new IOException("Failed to fetch IP address: " + response.code());
      }
      return response.body().string();
    }
  }

  @SneakyThrows
  @Override
  public String getIp() {
    String token = fetchIMDSToken();
    String ip = fetchPrivateIp(token);
    log.info("Found ip: " + ip);
    return ip;
  }
}
