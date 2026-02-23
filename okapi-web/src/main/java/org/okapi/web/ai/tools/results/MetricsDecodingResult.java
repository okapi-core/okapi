package org.okapi.web.ai.tools.results;

import java.util.List;

/**
 * Standard wrapper for metric query decoding results.
 *
 * <p>Aligns with the test fixtures which expect a batch containing the query id and the decoded
 * metric results plus any decoding errors.
 */
public record MetricsDecodingResult(MetricBatch batch, List<String> decodingErrors) {

  public record MetricBatch(String qid, List<MetricResult> result) {}
}
