/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.rest.traces;

import lombok.*;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class DbFilters {
  String system;
  String collection;
  String namespace;
  String operation;
}
