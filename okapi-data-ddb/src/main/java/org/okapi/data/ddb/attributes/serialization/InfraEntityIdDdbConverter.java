/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.data.ddb.attributes.serialization;

import org.okapi.data.ddb.attributes.InfraEntityId;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;

public class InfraEntityIdDdbConverter extends GenericCustomSerializerConverter<InfraEntityId> {
  @Override
  public CustomSerializer<InfraEntityId> getSerializer() {
    return InfraEntityIdSerializer.getSingleton();
  }

  @Override
  public EnhancedType<InfraEntityId> type() {
    return EnhancedType.of(InfraEntityId.class);
  }
}
