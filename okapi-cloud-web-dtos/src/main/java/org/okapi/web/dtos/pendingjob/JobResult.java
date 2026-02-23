/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.web.dtos.pendingjob;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.okapi.web.dtos.federation.StringList;
import org.okapi.web.dtos.federation.TimeMatrix;
import org.okapi.web.dtos.federation.TimeVector;

@Getter
@Setter
@NoArgsConstructor
public class JobResult {
  TimeMatrix timeMatrix;
  TimeVector timeVector;
  StringList stringList;
}
