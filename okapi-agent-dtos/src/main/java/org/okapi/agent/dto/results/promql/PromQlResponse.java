package org.okapi.agent.dto.results.promql;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class PromQlResponse<T> {
    String status;
    T data;
    String error;
    String errorType;
    List<String> warnings;
    List<String> infos;
}
