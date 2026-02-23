/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.web.ai.tools;

import java.util.List;

public interface GetDependenciesToolkit extends AiToolkit {
  List<String> getDependencies(String serviceName, long start, long end);
}
