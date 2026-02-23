/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.ops;

import java.util.concurrent.Callable;
import org.okapi.data.CreateDynamoDBTables;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(
    name = "ddb-migrate",
    description = "Create DynamoDB tables.",
    header = "Example: okapi-ops ddb-migrate --env local --endpoint http://localhost:4566")
public class DdbMigrateCommand implements Callable<Integer> {
  @Option(names = "--env", description = "Environment (local or prod).", defaultValue = "local")
  private String env;

  @Option(names = "--endpoint", description = "DynamoDB endpoint override.")
  private String endpoint;

  @Option(names = "--region", description = "AWS region.", defaultValue = "eu-west-2")
  private String region;

  @Override
  public Integer call() {
    var resolvedEndpoint = endpoint;
    if (resolvedEndpoint == null || resolvedEndpoint.isBlank()) {
      if ("prod".equals(env)) {
        resolvedEndpoint = "https://dynamodb.eu-west-2.amazonaws.com";
      } else {
        resolvedEndpoint = "http://localhost:4566";
      }
    }
    try (var dynamoDb = CreateDynamoDBTables.getDynamoDBClient(resolvedEndpoint, region)) {
      CreateDynamoDBTables.createTables(dynamoDb);
    }
    return 0;
  }
}
