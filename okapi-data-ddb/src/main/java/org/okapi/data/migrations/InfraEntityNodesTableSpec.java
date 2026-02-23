package org.okapi.data.migrations;

import static org.okapi.data.TableSpecifications.makeRequest;
import static org.okapi.data.dto.TableAttributes.INFRA_ENTITY_ID;
import static org.okapi.data.dto.TablesAndIndexes.INFRA_ENTITY_NODES_TABLE;

import java.util.Collections;
import org.okapi.data.dto.InfraEntityNodeDdb;
import org.okapi.data.TableSpec;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;

public class InfraEntityNodesTableSpec implements TableSpec<InfraEntityNodeDdb> {
  @Override
  public CreateTableRequest getSpec() {
    return makeRequest(
        INFRA_ENTITY_NODES_TABLE,
        INFRA_ENTITY_ID,
        null,
        Collections.emptyList(),
        Collections.emptyList(),
        Collections.emptyList());
  }

  @Override
  public String getName() {
    return INFRA_ENTITY_NODES_TABLE;
  }
}

