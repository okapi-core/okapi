package org.okapi.web.investigation.output;

import lombok.Builder;
import lombok.Getter;

@Getter
public class InvestigationOutput {
  String heading;
  String supportingEvidence;
  String refutingEvidence;
  String conclusion;

  @Builder
  public InvestigationOutput(
      String heading, String supportingEvidence, String refutingEvidence, String conclusion) {
    this.heading = heading;
    this.supportingEvidence = supportingEvidence;
    this.refutingEvidence = refutingEvidence;
    this.conclusion = conclusion;
  }
}
