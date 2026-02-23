package org.okapi.spring.configs.properties;

import jakarta.validation.constraints.Min;
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
@ConfigurationProperties(prefix = "okapi.cluster")
@Profile(Profiles.PROFILE_OKAPI_ENGINE)
@Component
@Validated
public class OkapiClusterCfg {
  @NotNull(message = "clusterId cannot be null")
  @NotBlank(message = "clusterId cannot be blank")
  String clusterId;

  @Min(value = 1000, message = "streamRouterCacheMillis must be ≥ 1000ms")
  int streamRouterCacheMillis;

  @NotNull(message = "namespace cannot be null")
  @NotBlank(message = "namespace cannot be blank")
  String namespace;
}
