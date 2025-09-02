package org.okapi.promql.eval.labelmatch;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.okapi.promql.parse.LabelMatcher;

import java.util.List;

@AllArgsConstructor
public class LabelConditionList extends LabelMatchCtx{
    @Getter
    List<LabelMatcher> conditions;
    @Override
    public TYPE getType() {
        return TYPE.LABEL_CONDITION_LIST;
    }
}
