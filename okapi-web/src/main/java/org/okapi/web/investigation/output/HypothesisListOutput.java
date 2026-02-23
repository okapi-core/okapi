package org.okapi.web.investigation.output;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class HypothesisListOutput {
  List<HypothesisOutput> hypothesisOutputs;
}
