package org.okapi.data.ddb;

import java.util.List;
import java.util.Optional;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

public class CommonQueryPatterns<T> {
  public List<T> listByPartitionKey(
      DynamoDbEnhancedClient enhancedClient, DynamoDbTable<T> table, AttributeValue pkValue) {
    var queryConditional = QueryConditional.keyEqualTo(k -> k.partitionValue(pkValue));
    var results = table.query(r -> r.queryConditional(queryConditional));
    return results.items().stream().toList();
  }

  public Optional<T> getByCompositeKey(
      DynamoDbEnhancedClient enhancedClient,
      DynamoDbTable<T> table,
      AttributeValue pkValue,
      AttributeValue skValue) {
    var key =
        software.amazon.awssdk.enhanced.dynamodb.Key.builder()
            .partitionValue(pkValue)
            .sortValue(skValue)
            .build();
    return Optional.ofNullable(table.getItem(r -> r.key(key)));
  }

  public void deleteByCompositeKey(
      DynamoDbEnhancedClient enhancedClient,
      DynamoDbTable<T> table,
      AttributeValue pkValue,
      AttributeValue skValue) {
    var key =
        software.amazon.awssdk.enhanced.dynamodb.Key.builder()
            .partitionValue(pkValue)
            .sortValue(skValue)
            .build();
    table.deleteItem(r -> r.key(key));
  }
}
