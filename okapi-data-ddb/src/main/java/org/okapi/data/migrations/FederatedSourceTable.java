package org.okapi.data.migrations;

import static org.okapi.data.TableSpecifications.makeRequest;

import java.util.Collections;
import org.okapi.data.dto.FederatedSource;
import org.okapi.data.dto.TableAttributes;
import org.okapi.data.dto.TablesAndIndexes;
import org.okapi.data.TableSpec;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;

public class FederatedSourceTable implements TableSpec<FederatedSource> {
  @Override
  public CreateTableRequest getSpec() {
    // Partition key must match the entity's @DynamoDbPartitionKey (TableAttributes.ORG_ID).
    // Using ORG_NAME here causes PutItem to miss the required key and fail at runtime.
    return makeRequest(
        TablesAndIndexes.FEDERATED_SOURCES_TABLE,
        TableAttributes.ORG_ID,
        TableAttributes.SOURCE_ID,
        Collections.emptyList(),
        Collections.emptyList(),
        Collections.emptyList());
  }

  @Override
  public String getName() {
    return TablesAndIndexes.FEDERATED_SOURCES_TABLE;
  }
}
