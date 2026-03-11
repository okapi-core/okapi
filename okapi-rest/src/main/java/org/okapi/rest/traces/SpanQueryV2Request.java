/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.rest.traces;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.*;

import java.util.List;

@Builder
@AllArgsConstructor
@JsonClassDescription(
    "Encapsulates search filters used to discover spans. Filters are composed via an AND operation i.e. only spans which match all filters are returned."
        + "Not every filter needs to be set. When in doubt, set the minimum sure set of filters.")
@Getter
@NoArgsConstructor
@ToString
public class SpanQueryV2Request {
  @JsonPropertyDescription(
      "Filter to spans belonging to this exact trace ID. TraceIDs are hex-encoded byte strings.")
  String traceId;

  @JsonPropertyDescription(
      "Filter to the span with this exact span ID. SpanIds are hex-encoded byte strings.")
  String spanId;

  @JsonPropertyDescription(
      """
  Span kind to filter. Usually the values are SPAN_KIND_SERVER or SPAN_KIND_CLIENT.
  If spans are not found, its likely this field wasn't set in the spans submitted by an application.
  Its a good idea to not set this filter in case no spans are found.
  """)
  String kind;

  @JsonPropertyDescription(
      """
  Filters scoped to database spans: system, collection, namespace, and operation.
  """)
  DbFilters dbFilters;

  @JsonPropertyDescription(
      "Restricts results to spans whose duration falls within the given millisecond range.")
  DurationFilter durationFilter;

  @JsonPropertyDescription("Filters scoped to HTTP spans: method, status code, origin, and host.")
  HttpFilters httpFilters;

  @JsonPropertyDescription("Filters by originating service name and/or downstream peer service.")
  ServiceFilter serviceFilter;

  @JsonPropertyDescription("Time window for the query, expressed in nanoseconds since Unix epoch.")
  TimestampFilter timestampFilter;

  @JsonPropertyDescription(
      "List of span attribute filters where each entry matches a string attribute key to an exact string value.")
  List<StringAttributeFilter> stringAttributesFilter;

  @JsonPropertyDescription(
      "List of span attribute filters where each entry matches a string attribute key to an exact numeric value.")
  List<NumberAttributeFilter> numberAttributesFilter;
}
