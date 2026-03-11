package org.okapi.oscar.integ;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.okapi.oscar.integ.corpus.SingleStepFlowCorpus;
import org.okapi.oscar.integ.judge.Judgment;

class SingleStepIntegrationTest extends OscarIntegTestBase {

  @BeforeAll
  void seedData() {
    new SingleStepFlowCorpus(ingesterClient).seed();
  }

  @Test
  void getMetricsForCheckoutService() {
    String session = session();
    String question =
        "Get metrics for the checkout service over the last hour. I need values for: "
            + String.join(", ", SingleStepFlowCorpus.METRIC_PATHS);
    oscarAi.postMessage(session, msg(question));
    assertThat(
            judgeAgent.judge(
                question,
                "Metric values found for checkout.http.requests, checkout.jvm.heap, and checkout.cpu.usage for the checkout service",
                getLatest(session)))
        .isNotEqualTo(Judgment.WRONG);
  }

  @Test
  void getErrorTracesForPaymentService() {
    String session = session();
    String question =
        "Find error traces for the " + SingleStepFlowCorpus.PAYMENT_SERVICE + " from the last hour";
    oscarAi.postMessage(session, msg(question));
    assertThat(
            judgeAgent.judge(
                question,
                "Found error traces for payment-service with HTTP 500 status codes",
                getLatest(session)))
        .isNotEqualTo(Judgment.WRONG);
  }

  @Test
  void getTracesByDatabaseSystem() {
    String session = session();
    String question =
        "Find trace IDs for spans that used postgresql as their database system in the last hour";
    oscarAi.postMessage(session, msg(question));
    assertThat(
            judgeAgent.judge(
                question,
                "Found trace IDs for spans with db.system=postgresql",
                getLatest(session)))
        .isNotEqualTo(Judgment.WRONG);
  }

  @Test
  void getRedMetricsForGatewayService() {
    String session = session();
    String question =
        "Get RED metrics for the "
            + SingleStepFlowCorpus.GATEWAY_SERVICE
            + " service for the last hour";
    oscarAi.postMessage(session, msg(question));
    assertThat(
            judgeAgent.judge(
                question,
                "RED metrics for api-gateway show request rate, error rate, and duration percentiles",
                getLatest(session)))
        .isNotEqualTo(Judgment.WRONG);
  }

  @Test
  void getRedMetricsForGatewayOperation() {
    String session = session();
    String question =
        "Get RED metrics for the "
            + SingleStepFlowCorpus.GATEWAY_OPERATION
            + " operation on the "
            + SingleStepFlowCorpus.GATEWAY_SERVICE
            + " service for the last hour";
    oscarAi.postMessage(session, msg(question));
    assertThat(
            judgeAgent.judge(
                question,
                "RED metrics for POST /checkout on api-gateway show request rate, error rate, and duration breakdown per operation",
                getLatest(session)))
        .isNotEqualTo(Judgment.WRONG);
  }
}
