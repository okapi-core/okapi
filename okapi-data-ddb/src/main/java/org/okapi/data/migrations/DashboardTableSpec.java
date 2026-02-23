/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.data.migrations;

import static org.okapi.data.TableSpecifications.makeRequest;
import static org.okapi.data.dto.TableAttributes.DASHBOARD_ID;
import static org.okapi.data.dto.TableAttributes.ORG_ID;
import static org.okapi.data.dto.TablesAndIndexes.DASHBOARDS_TABLE;

import java.util.Collections;
import org.okapi.data.TableSpec;
import org.okapi.data.dto.DashboardDdb;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;

public class DashboardTableSpec implements TableSpec<DashboardDdb> {
  @Override
  public CreateTableRequest getSpec() {

    return makeRequest(
        DASHBOARDS_TABLE,
        ORG_ID,
        DASHBOARD_ID,
        Collections.emptyList(),
        Collections.emptyList(),
        Collections.emptyList());
  }

  @Override
  public String getName() {
    return DASHBOARDS_TABLE;
  }
}
