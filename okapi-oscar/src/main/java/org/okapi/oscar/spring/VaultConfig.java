package org.okapi.oscar.spring;

import org.okapi.oscar.spring.cfg.VaultCfg;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.vault.authentication.TokenAuthentication;
import org.springframework.vault.client.VaultEndpoint;
import org.springframework.vault.core.VaultTemplate;

import java.net.URI;

@Configuration
public class VaultConfig {
  @Bean
  @ConditionalOnProperty(prefix = "okapi.oscar.vault", name = "address")
  public VaultTemplate vaultTemplate(@Autowired VaultCfg vaultCfg) {
    String address = vaultCfg.getAddress();
    String token = vaultCfg.getToken();
    if (address.isBlank()) {
      return null;
    }
    if (token == null || token.isBlank()) {
      throw new IllegalStateException("Vault token must be configured when using Vault");
    }
    VaultEndpoint endpoint = VaultEndpoint.from(URI.create(address));
    return new VaultTemplate(endpoint, new TokenAuthentication(token));
  }
}
