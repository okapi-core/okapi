package org.okapi.rest.metrics;


import lombok.*;

@Builder
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class SubmitMetricsResponse {
    String message;
}
