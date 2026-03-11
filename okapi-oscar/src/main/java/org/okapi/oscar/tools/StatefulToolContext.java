package org.okapi.oscar.tools;

public class StatefulToolContext {
  private final StatefulTools statefulTools;
  private final MetricsTools metricsTools;
  private final TracingTools tracingTools;

  public StatefulToolContext(
      StatefulTools statefulTools, MetricsTools metricsTools, TracingTools tracingTools) {
    this.statefulTools = statefulTools;
    this.metricsTools = metricsTools;
    this.tracingTools = tracingTools;
  }

  public StatefulTools getStatefulTools() {
    return statefulTools;
  }

  public MetricsTools getMetricsTools() {
    return metricsTools;
  }

  public TracingTools getTracingTools() {
    return tracingTools;
  }
}
