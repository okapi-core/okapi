/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.web.investigation.output;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class HypothesisOutput {
  @SerializedName("description")
  String description;

  @SerializedName("preliminary_supporting_evidence")
  String preliminarySupportingEvidence;

  @SerializedName("preliminary_refuting_evidence")
  String preliminaryRefutingEvidence;

  @SerializedName("confidence_level")
  String confidenceLevel;
}
