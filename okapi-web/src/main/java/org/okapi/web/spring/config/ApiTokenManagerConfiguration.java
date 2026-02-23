package org.okapi.web.spring.config;

import com.auth0.jwt.algorithms.Algorithm;
import org.okapi.web.auth.ApiTokenManager;
import org.okapi.web.auth.TokenDecoder;
import org.okapi.web.secrets.SecretsManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApiTokenManagerConfiguration {

  @Bean
  public ApiTokenManager apiTokenManager(
      @Autowired SecretsManager secretsManager, @Autowired TokenDecoder tokenDecoder) {

    var alg = Algorithm.HMAC256(secretsManager.getApiKey());
    return new ApiTokenManager(alg, tokenDecoder);
  }
}
