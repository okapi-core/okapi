package org.okapi.agent.dto.results.promql;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class PromQlInstantResult {
    Map<String, String> metric;
    List<Object> value;
}
