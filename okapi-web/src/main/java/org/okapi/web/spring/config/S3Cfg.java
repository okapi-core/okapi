package org.okapi.web.spring.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Data
@NoArgsConstructor
@ConfigurationProperties(prefix = "okapi.s3")
@Component
@Validated
public class S3Cfg {
  @NotNull @NotBlank String bucket;
  @NotNull @NotBlank String resultsPrefix;
}
