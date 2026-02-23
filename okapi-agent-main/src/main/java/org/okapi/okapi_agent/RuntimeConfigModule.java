package org.okapi.okapi_agent;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import lombok.AllArgsConstructor;
import org.okapi.okapi_agent.config.GatewayConfig;
import org.okapi.okapi_agent.jobhandler.HandlerCfg;
import org.okapi.okapi_agent.jobhandler.PendingJobRunner;

@AllArgsConstructor
public class RuntimeConfigModule extends AbstractModule {
  CmdParser.StaticConfig staticConfig;

  @Provides
  @Singleton
  public HandlerCfg handlerCfg() {
    return staticConfig.handlerCfg();
  }

  @Provides
  @Singleton
  public GatewayConfig gatewayConfig() {
    return staticConfig.gatewayConfig();
  }

  @Provides
  @Singleton
  public PendingJobRunner.PendingJobConfig pendingJobConfig() {
    return staticConfig.pendingJobConfig();
  }
}
