package org.okapi.promql.parse;

// parse/LabelsUtil.java
import java.util.*;

public final class LabelsUtil {
    private LabelsUtil() {}
    public static Map<String,String> toMap(List<LabelMatcher> matchers) {
        // Only equals matchers can be turned into concrete tags; others need SeriesDiscovery to expand.
        Map<String,String> m = new HashMap<>();
        for (LabelMatcher lm : matchers) {
            if (lm.op() == LabelOp.EQ) m.put(lm.name(), lm.value());
        }
        return m;
    }
}

