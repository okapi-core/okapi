/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.data.dto;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.okapi.data.ddb.attributes.InfraEntityId;
import org.okapi.data.ddb.attributes.InfraNodeOutgoingEdges;
import org.okapi.data.ddb.attributes.serialization.InfraEntityIdDdbConverter;
import org.okapi.data.ddb.attributes.serialization.InfraNodeOutgoingEdgesSerializer;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbConvertedBy;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@DynamoDbBean
public class InfraEntityNodeDdb {
  InfraEntityId infraEntityId;
  String attributes;
  InfraNodeOutgoingEdges outgoingEdges;

  @DynamoDbPartitionKey
  @DynamoDbAttribute(value = TableAttributes.INFRA_ENTITY_ID)
  @DynamoDbConvertedBy(InfraEntityIdDdbConverter.class)
  public InfraEntityId getInfraEntityId() {
    return infraEntityId;
  }

  @DynamoDbAttribute(value = TableAttributes.INFRA_ENTITY_ATTRIBUTES)
  public String getAttributes() {
    return attributes;
  }

  @DynamoDbAttribute(value = TableAttributes.INFRA_NODE_OUTGOING_EDGES)
  @DynamoDbConvertedBy(InfraNodeOutgoingEdgesSerializer.class)
  public InfraNodeOutgoingEdges getOutgoingEdges() {
    return outgoingEdges;
  }
}
