package org.okapi.rest.search;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Builder
public class LabelValueFilter {
  @JsonPropertyDescription("The label to which this value must be assigned.")
  String label;

  @JsonPropertyDescription("Value that should be assigned to this label.")
  String value;
}
