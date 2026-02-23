package org.okapi.web.secrets;

public interface SecretsManager {
  String getHmacKey();
  String getApiKey();
}
