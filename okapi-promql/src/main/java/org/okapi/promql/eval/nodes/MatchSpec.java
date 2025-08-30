package org.okapi.promql.eval.nodes;

// eval/match/MatchSpec.java
import java.util.*;

public final class MatchSpec {
    public enum Mode { ON, IGNORING }
    public final Mode mode;
    public final List<String> labels;     // key labels for matching/not matching
    public final boolean groupLeft;
    public final boolean groupRight;
    public final List<String> include;    // extra labels to include in expansion (optional)

    public MatchSpec(Mode mode, List<String> labels, boolean groupLeft, boolean groupRight, List<String> include) {
        this.mode = mode; this.labels = labels; this.groupLeft = groupLeft; this.groupRight = groupRight; this.include = include;
    }
}

