/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.data.dashboardvars;

import org.okapi.data.ddb.attributes.serialization.GenericGsonObjConverter;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;

public class DashVarConverterGson extends GenericGsonObjConverter<DashVars> {
  @Override
  public Class<DashVars> getClazz() {
    return DashVars.class;
  }

  @Override
  public EnhancedType<DashVars> type() {
    return EnhancedType.of(DashVars.class);
  }
}
