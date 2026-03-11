package org.okapi.oscar.secrets;

import lombok.extern.slf4j.Slf4j;

import java.util.Objects;

@Slf4j
public class EnvApiKeyProvider implements ApiKeyProvider {
  private final String envVar;

  public EnvApiKeyProvider(String envVar) {
    this.envVar = Objects.requireNonNull(envVar, "envVar");
  }

  @Override
  public String getKey() {
    log.info("Looking for var: {}", envVar);
    String value = System.getenv(envVar);
    if (value == null || value.isBlank()) {
      throw new IllegalStateException("Missing key in environment variables: " + envVar);
    }
    return value;
  }
}
