package org.okapi.data;

import static org.okapi.data.dto.TableAttributes.*;
import static org.okapi.data.dto.TablesAndIndexes.*;

import com.google.common.collect.Lists;
import java.util.*;
import software.amazon.awssdk.services.dynamodb.model.*;

public class TableSpecifications {

  public static CreateTableRequest getUsersTableSpecification() {
    return makeRequest(
        USERS_TABLE,
        USER_ID,
        null,
        Arrays.asList(USERS_BY_EMAIL_GSI),
        Arrays.asList(EMAIL),
        nullRangeKeys(1));
  }

  public static CreateTableRequest getUserRelationsDtoSpec() {
    return makeRequest(
        USER_ROLE_RELATIONS,
        USER_ID,
        USER_ROLE,
        Lists.newArrayList(ROLE_TO_USER_GSI),
        Lists.newArrayList(USER_ROLE),
        nullRangeKeys(1));
  }

  public static CreateTableRequest getTeamsTableSpecification() {
    return makeRequest(
        TEAMS_TABLE,
        TEAM_ID,
        null,
        Lists.newArrayList(ORG_TO_TEAM_GSI),
        Lists.newArrayList(ORG_ID),
        nullRangeKeys(1));
  }

  public static CreateTableRequest getTeamMembersTableSpecification() {
    return makeRequest(
        TEAM_MEMBERS_TABLE,
        TEAM_ID,
        USER_ID,
        Collections.emptyList(),
        Collections.emptyList(),
        Collections.emptyList());
  }

  public static CreateTableRequest getAuthorizationTokensTableSpecification() {
    return makeRequest(
        AUTHORIZATION_TOKENS_TABLE,
        AUTHORIZATION_TOKEN_ID,
        null,
        Lists.newArrayList(TEAM_TO_AUTHORIZATION_TOKEN_GSI),
        Lists.newArrayList(TEAM_ID),
        nullRangeKeys(1));
  }

  public static CreateTableRequest getOrgsTableSpecification() {
    return makeRequest(
        ORGS_TABLE,
        ORG_ID,
        null,
        Collections.emptyList(),
        Collections.emptyList(),
        Collections.emptyList());
  }

  public static CreateTableRequest getDashboardsTableSpec() {
    return makeRequest(
        DASHBOARDS_TABLE,
        DASHBOARD_ID,
        null,
        Collections.emptyList(),
        Collections.emptyList(),
        Collections.emptyList());
  }

  public static CreateTableRequest getRelationshipTableSpec() {
    return makeRequest(
        RELATIONSHIP_GRAPH_TABLE,
        ENTITY_ID,
        RELATED_ENTITY,
        Collections.emptyList(),
        Collections.emptyList(),
        Collections.emptyList());
  }

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
