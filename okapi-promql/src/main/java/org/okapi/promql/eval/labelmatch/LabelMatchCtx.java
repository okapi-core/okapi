package org.okapi.promql.eval.labelmatch;

public abstract class LabelMatchCtx {
  public enum TYPE {
    LABEL_CONDITION,
    LABEL_CONDITION_LIST,
    METRIC_MATCH_CONDITION
  }

  public abstract TYPE getType();
}
