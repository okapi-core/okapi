package org.okapi.rest.metrics.search;

import lombok.Builder;
import lombok.Getter;
import org.okapi.rest.metrics.MetricsPathSpecifier;

import java.util.List;

@Builder
@Getter
public class SearchMetricsResponse {
    List<MetricsPathSpecifier> results;
    int serverErrorCount;
    List<String> clientErrors;
}
