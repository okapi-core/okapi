package org.okapi.promql.eval.labelmatch;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.okapi.promql.parse.LabelMatcher;

import java.util.List;

@AllArgsConstructor
@Getter
public class MetricMatchCondition extends LabelMatchCtx{
    String metricNameOrNull;
    List<LabelMatcher> labelMatchers;

    @Override
    public TYPE getType() {
        return TYPE.METRIC_MATCH_CONDITION;
    }
}
