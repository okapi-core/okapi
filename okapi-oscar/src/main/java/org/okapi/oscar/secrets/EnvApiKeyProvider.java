package org.okapi.oscar.secrets;

import java.util.Objects;

public class EnvApiKeyProvider implements ApiKeyProvider {
  private final String envVar;

  public EnvApiKeyProvider(String envVar) {
    this.envVar = Objects.requireNonNull(envVar, "envVar");
  }

  @Override
  public String getKey() {
    String value = System.getenv(envVar);
    if (value == null || value.isBlank()) {
      throw new IllegalStateException("Missing key in environment variables: " + envVar);
    }
    return value;
  }
}
