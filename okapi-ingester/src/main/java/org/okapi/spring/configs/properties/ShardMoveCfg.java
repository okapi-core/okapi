package org.okapi.spring.configs.properties;

import jakarta.validation.constraints.Min;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.okapi.spring.configs.Profiles;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Data
@NoArgsConstructor
@Profile(Profiles.PROFILE_OKAPI_ENGINE)
@ConfigurationProperties(prefix = "okapi.sharding")
@Component
@Validated
public class ShardMoveCfg {
  @Min(value = 1, message = "walAckDurMillis must be > 0")
  long walAckDurMillis;
}
