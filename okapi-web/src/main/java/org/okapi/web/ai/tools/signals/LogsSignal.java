/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.web.ai.tools.signals;

import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;
import org.okapi.web.ai.tools.params.LogQuery;

@Getter
public class LogsSignal {
  LogQuery logQuery;
  List<LogsDocument> documents;

  @Builder
  protected LogsSignal(LogQuery query, @Singular List<LogsDocument> documents) {
    this.logQuery = query;
    this.documents = documents;
  }
}
