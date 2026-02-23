package org.okapi.resources;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@AllArgsConstructor
@Getter
@Builder
public class ResourceSearchQueryTemplate {
  String table;
  long startMs;
  long endMs;
}
