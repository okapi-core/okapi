package org.okapi.rest.metrics.payloads;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

@AllArgsConstructor
@Value
@Builder
public class Gauge {
    long[] ts;
    float[] value;
}
