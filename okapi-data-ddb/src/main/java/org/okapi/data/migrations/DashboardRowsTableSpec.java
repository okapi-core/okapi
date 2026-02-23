package org.okapi.data.migrations;

import static org.okapi.data.TableSpecifications.makeRequest;
import static org.okapi.data.dto.TableAttributes.ORG_DASH_KEY;
import static org.okapi.data.dto.TableAttributes.ROW_ID;
import static org.okapi.data.dto.TablesAndIndexes.DASHBOARD_ROWS_TABLE;

import java.util.Collections;
import org.okapi.data.dto.DashboardRow;
import org.okapi.data.TableSpec;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;

public class DashboardRowsTableSpec implements TableSpec<DashboardRow> {
  @Override
  public CreateTableRequest getSpec() {
    return makeRequest(
        DASHBOARD_ROWS_TABLE,
        ORG_DASH_KEY,
        ROW_ID,
        Collections.emptyList(),
        Collections.emptyList(),
        Collections.emptyList());
  }

  @Override
  public String getName() {
    return DASHBOARD_ROWS_TABLE;
  }
}

