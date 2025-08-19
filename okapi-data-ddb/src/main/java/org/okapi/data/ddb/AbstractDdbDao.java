package org.okapi.data.ddb;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

public abstract class AbstractDdbDao<T extends Object, D> {

  DynamoDbTable<T> table;
  DynamoDbEnhancedClient dynamoDbEnhancedClient;

  public AbstractDdbDao(
      String table, DynamoDbEnhancedClient dynamoDbEnhancedClient, Class<T> clazz) {
    this.dynamoDbEnhancedClient = dynamoDbEnhancedClient;
    this.table = dynamoDbEnhancedClient.table(table, TableSchema.fromBean(clazz));
  }

  public abstract T fromDto(D dto);

  public abstract D toDto(T obj);
  
}
