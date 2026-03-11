/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.rest.traces;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.*;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@ToString
public class DbFilters {
  @JsonPropertyDescription(
      "Database system (e.g. postgresql, redis, mongodb). Maps to the db.system span attribute.")
  String system;

  @JsonPropertyDescription(
      "Database collection or table name. Maps to the db.mongodb.collection or equivalent span attribute.")
  String collection;

  @JsonPropertyDescription("Database namespace or schema. Maps to the db.name span attribute.")
  String namespace;

  @JsonPropertyDescription(
      "Database operation type (e.g. SELECT, INSERT, find). Maps to the db.operation span attribute.")
  String operation;
}
