/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.web.service.federation.converter;

import java.util.HashMap;
import java.util.List;
import org.okapi.web.dtos.federation.StringList;
import org.okapi.web.dtos.federation.TimeMatrix;
import org.okapi.web.dtos.federation.TimeVector;
import org.springframework.stereotype.Service;

@Service
public class FederatedDataTypeFactoryImpl implements FederatedDataTypeFactory {
  @Override
  public TimeVector createTimeVector(org.okapi.web.tsvector.TimeVector vector) {
    var values = vector.values();
    var timestamps = vector.timestamps();
    return TimeVector.builder().values(values).timestamps(timestamps).build();
  }

  @Override
  public TimeMatrix createTimeMatrix(org.okapi.web.tsvector.TimeMatrix matrix) {
    var mat = new HashMap<String, TimeVector>();
    for (var path : matrix.getPaths()) {
      var vector = matrix.getTimeVector(path);
      mat.put(path, createTimeVector(vector));
    }
    return TimeMatrix.builder().timeVectors(mat).build();
  }

  @Override
  public StringList createStringList(List<String> strings) {
    return StringList.builder().strings(strings).build();
  }
}
