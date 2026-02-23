/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.data.ddb.attributes.serialization;

import org.okapi.data.ddb.attributes.EdgeAttributes;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;

public class EdgeAttributeConverter extends GenericGsonObjConverter<EdgeAttributes> {
  @Override
  public Class<EdgeAttributes> getClazz() {
    return EdgeAttributes.class;
  }

  @Override
  public EnhancedType<EdgeAttributes> type() {
    return EnhancedType.of(EdgeAttributes.class);
  }
}
