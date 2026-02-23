/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.web.dtos.federation;

import jakarta.validation.constraints.NotNull;
import java.util.Map;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class FederatedQueryResponse {

  @NotNull String sourceId;
  @NotNull String queryExecuted;
  @NotNull EXPECTED_RESULT resultType;
  String dedupKey;
  String legendFn;

  Map<String, Object> queryContext;
  TimeMatrix timeMatrix;
  TimeVector timeVector;
  StringList stringList;

  public void setResultType(EXPECTED_RESULT resultType) {
    if (this.resultType == resultType) {
      return;
    }
    if (this.resultType != EXPECTED_RESULT.NONE) {
      throw new IllegalArgumentException("Type conversion is not supported");
    }
    this.resultType = resultType;
  }

  public FederatedQueryResponse setTimeMatrix(TimeMatrix timeMatrix) {
    setResultType(EXPECTED_RESULT.TIME_MATRIX);
    this.timeMatrix = timeMatrix;
    return this;
  }

  public FederatedQueryResponse setTimeVector(TimeVector timeVector) {
    setResultType(EXPECTED_RESULT.TIME_VECTOR);
    this.timeVector = timeVector;
    return this;
  }

  public FederatedQueryResponse setStringList(StringList stringList) {
    setResultType(EXPECTED_RESULT.STR_LIST);
    this.stringList = stringList;
    return this;
  }
}
