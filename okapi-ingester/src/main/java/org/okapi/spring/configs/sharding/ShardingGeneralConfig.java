package org.okapi.spring.configs.sharding;

import org.okapi.CommonConfig;
import org.okapi.sharding.HashingShardAssigner;
import org.okapi.spring.configs.Profiles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile(Profiles.PROFILE_OKAPI_ENGINE)
public class ShardingGeneralConfig {
  @Bean
  public HashingShardAssigner shardAssigner() {
    return new HashingShardAssigner(CommonConfig.N_SHARDS);
  }
}
