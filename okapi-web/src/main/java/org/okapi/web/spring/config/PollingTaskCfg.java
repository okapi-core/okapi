package org.okapi.web.spring.config;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Data
@NoArgsConstructor
@ConfigurationProperties(prefix = "okapi.federation.polling-task")
@Component
@Validated
public class PollingTaskCfg {
  long initialDelayMs = 1000;
  int maxAttempts = 30;
  int threads = 5;
}
