package org.okapi.okapi_agent;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import org.okapi.okapi_agent.jobhandler.HandlerCfg;

import java.nio.file.Path;

public class HandlerCfgModule extends AbstractModule {
  Path handlerFile;

  public HandlerCfgModule(Path handlerFile) {
    this.handlerFile = handlerFile;
  }

  @Provides
  @Singleton
  public HandlerCfg provideHandlerCfg() {
    return new HandlerCfg(handlerFile);
  }
}
