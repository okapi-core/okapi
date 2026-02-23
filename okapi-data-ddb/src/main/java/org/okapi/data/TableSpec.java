package org.okapi.data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.ResourceInUseException;

public interface TableSpec<T> {
  Logger LOG = LoggerFactory.getLogger(TableSpec.class);

  CreateTableRequest getSpec();

  String getName();

  default void create(DynamoDbClient client) {
    var spec = getSpec();
    var tables = client.listTables();
    if (tables.tableNames().contains(spec.tableName())) {
      return;
    }
    try {
      client.createTable(spec);
    } catch (ResourceInUseException e) {
      // move on if this table already exists
      LOG.warn("Error creating table {}: {}", spec.tableName(), e.getMessage());
    }
  }
}
