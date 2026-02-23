package org.okapi.agent.dto.results.promql;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class PromQlArrayResponse<T> {
    String status;
    List<T> data;
}
