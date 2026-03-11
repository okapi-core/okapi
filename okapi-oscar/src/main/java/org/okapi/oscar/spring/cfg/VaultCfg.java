package org.okapi.oscar.spring.cfg;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "okapi.oscar.vault")
@Getter
@Setter
@NoArgsConstructor
public class VaultCfg {
  String address;
  String token;
}
