/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.data;

import com.google.common.collect.Lists;
import java.util.*;
import software.amazon.awssdk.services.dynamodb.model.*;

public class TableSpecifications {

  public static CreateTableRequest makeRequest(
      String tablename,
      String hashKey,
      String rangeKey,
      List<String> gsis,
      List<String> gsiAttributes,
      List<String> gsiRangeAttributes) {
    var gsiRequests = new ArrayList<GlobalSecondaryIndex>();

    for (int i = 0; i < gsis.size(); i++) {
      var gsiHashKey = gsiAttributes.get(i);
      var gsiRangeKey = gsiRangeAttributes.get(i);
      KeySchemaElement[] elements = new KeySchemaElement[(gsiRangeKey == null) ? 1 : 2];
      elements[0] =
          KeySchemaElement.builder().keyType(KeyType.HASH).attributeName(gsiHashKey).build();
      if (gsiRangeKey != null) {
        elements[1] =
            KeySchemaElement.builder().keyType(KeyType.RANGE).attributeName(gsiRangeKey).build();
      }

      var gsiRequest =
          GlobalSecondaryIndex.builder()
              .projection(Projection.builder().projectionType(ProjectionType.ALL).build())
              .keySchema(elements)
              .indexName(gsis.get(i))
              .build();
      gsiRequests.add(gsiRequest);
    }

    var allAttributes = new HashSet<String>();
    allAttributes.add(hashKey);
    if (rangeKey != null) {
      allAttributes.add(rangeKey);
    }
    allAttributes.addAll(gsiAttributes);
    allAttributes.addAll(gsiRangeAttributes);

    Collection<AttributeDefinition> attributeDefinitions =
        allAttributes.stream()
            .filter(Objects::nonNull)
            .map(TableSpecifications::stringAttribute)
            .toList();
    var keySchemaElements = new ArrayList<KeySchemaElement>();
    keySchemaElements.add(
        KeySchemaElement.builder().keyType(KeyType.HASH).attributeName(hashKey).build());
    if (rangeKey != null) {
      keySchemaElements.add(
          KeySchemaElement.builder().keyType(KeyType.RANGE).attributeName(rangeKey).build());
    }
    var request =
        CreateTableRequest.builder()
            .tableName(tablename)
            .billingMode(BillingMode.PAY_PER_REQUEST)
            .keySchema(keySchemaElements)
            .attributeDefinitions(attributeDefinitions);
    if (!gsis.isEmpty()) {
      GlobalSecondaryIndex[] gsiRequestArrr = new GlobalSecondaryIndex[gsiRequests.size()];
      gsiRequests.toArray(gsiRequestArrr);
      return request.globalSecondaryIndexes(gsiRequestArrr).build();
    } else return request.build();
  }

  public static List<String> nullRangeKeys(int n) {
    var list = Lists.<String>newArrayList();
    for (int i = 0; i < n; i++) {
      list.add(null);
    }
    return list;
  }

  public static AttributeDefinition stringAttribute(String name) {
    return AttributeDefinition.builder()
        .attributeName(name)
        .attributeType(ScalarAttributeType.S)
        .build();
  }
}
