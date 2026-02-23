package org.okapi.metrics.service;

/** register() -> register itself to the consumer farm freeze() -> freeze metrics consumption */
public interface MetricsHandler {
  /** When the consumer is ready to start consuming metrics */
  void onStart() throws Exception;

  /** Callback for when the consumer is stopped */
  void onStop() throws Exception;
}
