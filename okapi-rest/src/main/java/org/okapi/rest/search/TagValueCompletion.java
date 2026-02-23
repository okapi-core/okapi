package org.okapi.rest.search;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public class TagValueCompletion {
    String tag;
    List<String> candidates;
    MetricEventFilter metricEventFilter;
}
