/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.okapi_agent;

import com.google.inject.Guice;
import org.okapi.okapi_agent.jobhandler.JobHandlerModule;
import org.okapi.okapi_agent.jobhandler.PendingJobRunner;

public class OkapiAgentApplication {

  public static void main(String[] args) {
    var parser = CmdParser.fromArgs(args);
    var injector = Guice.createInjector(new JobHandlerModule(), new RuntimeConfigModule(parser));
    var scheduler = injector.getInstance(PendingJobRunner.class);
    scheduler.start();
  }
}
