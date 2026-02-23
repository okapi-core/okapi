/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.data.migrations;

import static org.okapi.data.TableSpecifications.makeRequest;
import static org.okapi.data.dto.TableAttributes.DASHBOARD_VERSION_ID;
import static org.okapi.data.dto.TableAttributes.ORG_ID;
import static org.okapi.data.dto.TablesAndIndexes.DASHBOARD_VERSIONS_TABLE;

import java.util.Collections;
import org.okapi.data.TableSpec;
import org.okapi.data.dto.DashboardVersion;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;

public class DashboardVersionsTableSpec implements TableSpec<DashboardVersion> {
  @Override
  public CreateTableRequest getSpec() {
    return makeRequest(
        DASHBOARD_VERSIONS_TABLE,
        ORG_ID,
        DASHBOARD_VERSION_ID,
        Collections.emptyList(),
        Collections.emptyList(),
        Collections.emptyList());
  }

  @Override
  public String getName() {
    return DASHBOARD_VERSIONS_TABLE;
  }
}
