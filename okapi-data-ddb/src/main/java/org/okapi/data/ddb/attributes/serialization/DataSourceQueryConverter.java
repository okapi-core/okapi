/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.data.ddb.attributes.serialization;

import org.okapi.data.dto.DataSourceQuery;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;

public class DataSourceQueryConverter extends GenericGsonObjConverter<DataSourceQuery> {
  @Override
  public Class<DataSourceQuery> getClazz() {
    return DataSourceQuery.class;
  }

  @Override
  public EnhancedType<DataSourceQuery> type() {
    return EnhancedType.of(DataSourceQuery.class);
  }
}
