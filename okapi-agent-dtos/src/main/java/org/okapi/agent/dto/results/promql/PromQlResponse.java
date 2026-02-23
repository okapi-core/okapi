/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.agent.dto.results.promql;

import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PromQlResponse<T> {
  String status;
  T data;
  String error;
  String errorType;
  List<String> warnings;
  List<String> infos;
}
