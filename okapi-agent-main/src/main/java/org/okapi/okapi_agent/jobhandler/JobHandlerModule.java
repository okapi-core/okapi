/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.okapi_agent.jobhandler;

import com.google.gson.Gson;
import com.google.inject.AbstractModule;
import lombok.AllArgsConstructor;
import okhttp3.OkHttpClient;

@AllArgsConstructor
public class JobHandlerModule extends AbstractModule {

  protected void configure() {
    bind(Gson.class).toInstance(new Gson());
    bind(OkHttpClient.class).toInstance(new OkHttpClient());
  }
}
