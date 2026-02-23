package org.okapi.data.migrations;

import static org.okapi.data.TableSpecifications.makeRequest;
import static org.okapi.data.dto.TableAttributes.ORG_PANEL_HASH_KEY;
import static org.okapi.data.dto.TableAttributes.PANEL_ID;
import static org.okapi.data.dto.TablesAndIndexes.DASHBOARD_PANELS_TABLE;

import java.util.Collections;
import org.okapi.data.dto.DashboardPanel;
import org.okapi.data.TableSpec;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;

public class DashboardPanelsTableSpec implements TableSpec<DashboardPanel> {
  @Override
  public CreateTableRequest getSpec() {
    return makeRequest(
        DASHBOARD_PANELS_TABLE,
        ORG_PANEL_HASH_KEY,
        PANEL_ID,
        Collections.emptyList(),
        Collections.emptyList(),
        Collections.emptyList());
  }

  @Override
  public String getName() {
    return DASHBOARD_PANELS_TABLE;
  }
}

