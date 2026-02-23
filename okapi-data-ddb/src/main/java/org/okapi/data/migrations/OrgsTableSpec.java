package org.okapi.data.migrations;

import static org.okapi.data.TableSpecifications.makeRequest;
import static org.okapi.data.dto.TableAttributes.ORG_ID;
import static org.okapi.data.dto.TablesAndIndexes.ORGS_TABLE;

import java.util.Collections;
import org.okapi.data.dto.OrgDtoDdb;
import org.okapi.data.TableSpec;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;

public class OrgsTableSpec implements TableSpec<OrgDtoDdb> {
  @Override
  public CreateTableRequest getSpec() {
    return makeRequest(
        ORGS_TABLE,
        ORG_ID,
        null,
        Collections.emptyList(),
        Collections.emptyList(),
        Collections.emptyList());
  }

  @Override
  public String getName() {
    return ORGS_TABLE;
  }
}
