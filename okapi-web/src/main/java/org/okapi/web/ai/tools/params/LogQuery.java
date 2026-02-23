/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.web.ai.tools.params;

import lombok.Getter;
import org.okapi.web.ai.tools.ResourcePath;
import org.okapi.web.ai.tools.filters.LabelFilter;
import org.okapi.web.ai.tools.filters.LevelFilter;
import org.okapi.web.ai.tools.filters.RegexFilter;

@Getter
public class LogQuery {
  ResourcePath resourcePath;
  long startTime;
  long endTime;
  LabelFilter labelFilter;
  LevelFilter levelFilter;
  RegexFilter regexFilter;
}
