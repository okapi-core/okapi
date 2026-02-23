/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.data.ddb.attributes.serialization;

import org.okapi.data.ddb.attributes.InfraNodeOutgoingEdges;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;

public class InfraNodeOutgoingEdgesSerializer
    extends GenericGsonObjConverter<InfraNodeOutgoingEdges> {
  @Override
  public Class<InfraNodeOutgoingEdges> getClazz() {
    return InfraNodeOutgoingEdges.class;
  }

  @Override
  public EnhancedType<InfraNodeOutgoingEdges> type() {
    return EnhancedType.of(InfraNodeOutgoingEdges.class);
  }
}
