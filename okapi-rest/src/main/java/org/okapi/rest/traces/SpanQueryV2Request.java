/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.rest.traces;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@AllArgsConstructor
public class SpanQueryV2Request {
  String traceId;
  String spanId;
  String kind;
  DbFilters dbFilters;
  DurationFilter durationFilter;
  HttpFilters httpFilters;
  ServiceFilter serviceFilter;
  TimestampFilter timestampFilter;
  List<StringAttributeFilter> stringAttributesFilter;
  List<NumberAttributeFilter> numberAttributesFilter;
}
