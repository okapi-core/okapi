package org.okapi.rest.chat.payload;

import org.okapi.rest.annotations.TsResponseType;
import org.okapi.rest.metrics.query.GetMetricsRequest;

@TsResponseType
public record PlotMetricFollowUpPayload(GetMetricsRequest request) {}
