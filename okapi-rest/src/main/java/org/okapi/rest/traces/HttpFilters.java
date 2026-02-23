package org.okapi.rest.traces;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class HttpFilters {
  String httpMethod;
  Integer statusCode;
  String origin;
  String host;
}
