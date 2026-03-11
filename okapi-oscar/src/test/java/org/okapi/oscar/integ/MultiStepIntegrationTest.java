package org.okapi.oscar.integ;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.okapi.oscar.integ.corpus.HighLatencyCpuSpikeCorpus;
import org.okapi.oscar.integ.judge.Judgment;

import static org.assertj.core.api.Assertions.assertThat;

class MultiStepIntegrationTest extends OscarIntegTestBase {

  @BeforeAll
  void seedData() {
    new HighLatencyCpuSpikeCorpus(ingesterClient).seed();
  }

  @Test
  void debugHighLatencyDueToCpuSpike() {
    String question =
        "The "
            + HighLatencyCpuSpikeCorpus.CHECKOUT_SERVICE
            + " service is experiencing high latency (>= 500ms). Debug the root cause. "
            + "The postgres database runs on host "
            + HighLatencyCpuSpikeCorpus.POSTGRES_HOST
            + ". Check recent slow traces and correlate with host metrics.";
    String session = session(question);
    var contents = pollUntilFinAndReturnResponse(session);
    assertThat(
            judgeAgent.judge(
                question,
                "Mentiones high latency on "
                    + HighLatencyCpuSpikeCorpus.POSTGRES_HOST
                    + ". Slow POST /checkout traces carry server.name="
                    + HighLatencyCpuSpikeCorpus.POSTGRES_HOST
                    + ", and container.cpu.percent on that host is at "
                    + HighLatencyCpuSpikeCorpus.HIGH_CPU_VALUE
                    + "%, indicating the postgres instance is CPU-saturated.",
                contents))
        .isNotEqualTo(Judgment.WRONG);
  }
}
