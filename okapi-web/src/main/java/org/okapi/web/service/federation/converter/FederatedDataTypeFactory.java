/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.web.service.federation.converter;

import java.util.List;
import org.okapi.web.dtos.federation.StringList;
import org.okapi.web.dtos.federation.TimeMatrix;
import org.okapi.web.dtos.federation.TimeVector;

public interface FederatedDataTypeFactory {

  TimeVector createTimeVector(org.okapi.web.tsvector.TimeVector vector);

  TimeMatrix createTimeMatrix(org.okapi.web.tsvector.TimeMatrix matrix);

  StringList createStringList(List<String> strings);
}
