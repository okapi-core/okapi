package org.okapi.data.migrations;

import static org.okapi.data.TableSpecifications.makeRequest;
import static org.okapi.data.dto.TableAttributes.EDGE_ID;
import static org.okapi.data.dto.TableAttributes.RELATED_ENTITY;
import static org.okapi.data.dto.TablesAndIndexes.RELATIONSHIP_GRAPH_TABLE;

import java.util.Collections;
import org.okapi.data.dto.RelationGraphNodeDdb;
import org.okapi.data.TableSpec;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;

public class RelationGraphDdbSpec implements TableSpec<RelationGraphNodeDdb> {
  @Override
  public CreateTableRequest getSpec() {
    return makeRequest(
        RELATIONSHIP_GRAPH_TABLE,
            EDGE_ID,
        RELATED_ENTITY,
        Collections.emptyList(),
        Collections.emptyList(),
        Collections.emptyList());
  }

  @Override
  public String getName() {
    return RELATIONSHIP_GRAPH_TABLE;
  }
}
