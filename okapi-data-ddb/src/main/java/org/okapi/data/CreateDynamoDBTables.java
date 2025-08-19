package org.okapi.data;

import static org.okapi.data.dto.TablesAndIndexes.*;

import java.net.URI;
import org.okapi.data.dto.TablesAndIndexes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

public class CreateDynamoDBTables {
  public static void createTables(DynamoDbClient dynamoDBClient) {
    var existingTables = dynamoDBClient.listTables().tableNames();
    if (!existingTables.contains(TablesAndIndexes.USERS_TABLE)) {
      dynamoDBClient.createTable(TableSpecifications.getUsersTableSpecification());
    }
    if (!existingTables.contains(USER_ROLE_RELATIONS)) {
      dynamoDBClient.createTable(TableSpecifications.getUserRelationsDtoSpec());
    }
    if (!existingTables.contains(TEAMS_TABLE)) {
      dynamoDBClient.createTable(TableSpecifications.getTeamsTableSpecification());
    }
    if (!existingTables.contains(TEAM_MEMBERS_TABLE)) {
      dynamoDBClient.createTable(TableSpecifications.getTeamMembersTableSpecification());
    }
    if (!existingTables.contains(AUTHORIZATION_TOKENS_TABLE)) {
      dynamoDBClient.createTable(TableSpecifications.getAuthorizationTokensTableSpecification());
    }
    if (!existingTables.contains(ORGS_TABLE))
      dynamoDBClient.createTable(TableSpecifications.getOrgsTableSpecification());
    if (!existingTables.contains(DASHBOARDS_TABLE)) {
      dynamoDBClient.createTable(TableSpecifications.getDashboardsTableSpec());
    }
    if (!existingTables.contains(RELATIONSHIP_GRAPH_TABLE)) {
      dynamoDBClient.createTable(TableSpecifications.getRelationshipTableSpec());
    }
  }

  public static DynamoDbClient getDynamoDBClient(String endpoint, String region) {
    return DynamoDbClient.builder()
        .endpointOverride(URI.create(endpoint))
        .region(Region.of(region))
        .build();
  }

  public static void main(String[] args) {
    var env = args[0];
    String endpoint = "http://localhost:4566";
    if (env.equals("prod")) {
      endpoint = "https://dynamodb.eu-west-2.amazonaws.com";
    }
    var dynamoDb = getDynamoDBClient(endpoint, "eu-west-2");
    createTables(dynamoDb);
  }
}
