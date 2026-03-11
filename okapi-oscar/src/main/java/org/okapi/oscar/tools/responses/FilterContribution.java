package org.okapi.oscar.tools.responses;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.Builder;
import lombok.Value;

import java.util.Map;

@Value
@Builder
@JsonClassDescription(
"""
Per-filter match counts for SpanQueryV2Request. A non-zero count means the filter alone matches some spans.
A count of 0 means the filter alone matches no spans. A null count means the filter was not set in the request.
""")
public class FilterContribution {
  @JsonPropertyDescription(
"""
Count of spans matching SpanQueryV2Request.traceId. Non-zero means matches exist, 0 means no matches, null means traceId was not set.
""")
  Long traceIdFilterResultCount;

  @JsonPropertyDescription(
"""
Count of spans matching SpanQueryV2Request.spanId. Non-zero means matches exist, 0 means no matches, null means spanId was not set.
""")
  Long spanIdFilterResultCount;

  @JsonPropertyDescription(
"""
Count of spans matching SpanQueryV2Request.kind. Non-zero means matches exist, 0 means no matches, null means kind was not set.
""")
  Long kindFilterCount;

  @JsonPropertyDescription(
"""
Count of spans matching SpanQueryV2Request.dbFilters. Non-zero means matches exist, 0 means no matches, null means dbFilters was not set.
""")
  Long dbFiltersCount;

  @JsonPropertyDescription(
"""
Count of spans matching SpanQueryV2Request.durationFilter. Non-zero means matches exist, 0 means no matches, null means durationFilter was not set.
""")
  Long durationFilterCount;

  @JsonPropertyDescription(
"""
Count of spans matching SpanQueryV2Request.httpFilters. Non-zero means matches exist, 0 means no matches, null means httpFilters was not set.
""")
  Long httpFiltersCount;

  @JsonPropertyDescription(
"""
Count of spans matching SpanQueryV2Request.serviceFilter. Non-zero means matches exist, 0 means no matches, null means serviceFilter was not set.
""")
  Long serviceFilterCount;

  @JsonPropertyDescription(
"""
Count of spans matching SpanQueryV2Request.timestampFilter. Non-zero means matches exist, 0 means no matches, null means timestampFilter was not set.
""")
  Long timestampFilterCount;

  @JsonPropertyDescription(
"""
Per-key counts for SpanQueryV2Request.stringAttributesFilter. Each entry maps a string attribute key to its count.
Non-zero means matches exist, 0 means no matches, null means stringAttributesFilter was not set.
""")
  Map<String, Long> stringAttributesFilterCount;

  @JsonPropertyDescription(
"""
Per-key counts for SpanQueryV2Request.numberAttributesFilter. Each entry maps a numeric attribute key to its count.
Non-zero means matches exist, 0 means no matches, null means numberAttributesFilter was not set.
""")
  Map<String, Long> numberAttributesFilterCount;
}
