/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.okapi_agent.jobhandler;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import java.nio.file.Path;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class HandlerCfgProvider extends AbstractModule {
  Path handlerCfgPath;

  @Provides
  @Singleton
  public HandlerCfg provideHandlerCfg() {
    return new HandlerCfg(handlerCfgPath);
  }
}
