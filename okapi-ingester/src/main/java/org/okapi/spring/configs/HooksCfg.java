package org.okapi.spring.configs;

import java.io.IOException;
import org.okapi.spring.hooks.CreateDirectoriesHook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile(Profiles.PROFILE_OKAPI_ENGINE)
public class HooksCfg {

  @Bean
  public CreateDirectoriesHook createDirectories(
      @Autowired CreateDirectoriesHook createDirectoriesHook) throws IOException {
    createDirectoriesHook.run();
    return createDirectoriesHook;
  }
}
