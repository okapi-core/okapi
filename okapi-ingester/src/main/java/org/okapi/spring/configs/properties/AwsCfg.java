package org.okapi.spring.configs.properties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.okapi.spring.configs.Profiles;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ConfigurationProperties(prefix = "okapi.aws")
@Profile(Profiles.PROFILE_OKAPI_ENGINE)
@Component
@Validated
public class AwsCfg {
  @NotBlank @NotNull String region;
  @NotBlank @NotNull String endpoint;
}
