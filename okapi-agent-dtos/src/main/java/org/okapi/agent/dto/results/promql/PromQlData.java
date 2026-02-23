package org.okapi.agent.dto.results.promql;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class PromQlData<T> extends AbstractPromQlData {
    T[] result;

    public PromQlData(String resultType) {
        super(resultType);
    }
}
