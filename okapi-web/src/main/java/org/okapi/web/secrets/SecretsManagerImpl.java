package org.okapi.web.secrets;

import static org.okapi.web.secrets.SecretPaths.SECRETS_BASE_PATH;

import com.google.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;

@Component
public class SecretsManagerImpl implements SecretsManager {

  SecretsBundle bundle;

  public SecretsManagerImpl(@Autowired SecretsManagerClient secretsManagerClient) {
    var getSecretRequest = GetSecretValueRequest.builder().secretId(SECRETS_BASE_PATH).build();
    var gson = new Gson();
    var getSecretResponse = secretsManagerClient.getSecretValue(getSecretRequest);
    bundle = gson.fromJson(getSecretResponse.secretString(), SecretsBundle.class);
  }

  public String getHmacKey() {
    return bundle.getHmacKey();
  }

  @Override
  public String getApiKey() {
    return bundle.getApiKey();
  }
}
