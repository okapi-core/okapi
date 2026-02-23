package org.okapi.spring.configs;

import org.okapi.queryproc.FanoutGrouper;
import org.okapi.routing.StreamRouter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile(Profiles.PROFILE_OKAPI_ENGINE)
public class StreamRouterConfig {

  @Bean
  public FanoutGrouper fanoutGrouper(@Autowired StreamRouter streamRouter) {
    return new FanoutGrouper(streamRouter);
  }
}
