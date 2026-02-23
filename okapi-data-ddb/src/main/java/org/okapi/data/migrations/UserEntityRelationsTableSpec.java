package org.okapi.data.migrations;

import static org.okapi.data.TableSpecifications.makeRequest;
import static org.okapi.data.dto.TableAttributes.EDGE_ID;
import static org.okapi.data.dto.TableAttributes.USER_ID;
import static org.okapi.data.dto.TablesAndIndexes.USER_ENTITY_RELATIONS_TABLE;

import org.okapi.data.dto.UserEntityRelations;
import org.okapi.data.TableSpec;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;

public class UserEntityRelationsTableSpec implements TableSpec<UserEntityRelations> {
  @Override
  public CreateTableRequest getSpec() {
    // Hash key: user_id, Range key: graph_entity_id
    return makeRequest(USER_ENTITY_RELATIONS_TABLE, USER_ID, EDGE_ID, java.util.List.of(), java.util.List.of(), java.util.List.of());
  }

  @Override
  public String getName() {
    return USER_ENTITY_RELATIONS_TABLE;
  }
}

