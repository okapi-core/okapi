/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.web.spring.config;

import com.auth0.jwt.algorithms.Algorithm;
import com.google.gson.Gson;
import okhttp3.OkHttpClient;
import org.okapi.web.secrets.SecretsManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WebAppConfiguration {

  @Bean
  public Algorithm alg(@Autowired SecretsManager secretsManager) {
    var secret = secretsManager.getHmacKey();
    return Algorithm.HMAC256(secret);
  }

  @Bean
  public OkHttpClient httpClient() {
    return new OkHttpClient();
  }

  @Bean
  public Gson gson() {
    return new Gson();
  }
}
