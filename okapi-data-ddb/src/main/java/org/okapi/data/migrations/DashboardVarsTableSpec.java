/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.data.migrations;

import static org.okapi.data.TableSpecifications.makeRequest;
import static org.okapi.data.dto.TablesAndIndexes.DASHBOARD_VAR_TABLE;

import java.util.Collections;
import org.okapi.data.TableSpec;
import org.okapi.data.dto.TableAttributes;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;

public class DashboardVarsTableSpec implements TableSpec<DashboardVarsTableSpec> {

  @Override
  public CreateTableRequest getSpec() {
    return makeRequest(
        DASHBOARD_VAR_TABLE,
        TableAttributes.ORG_DASH_KEY,
        TableAttributes.DASH_VAR_NAME,
        Collections.emptyList(),
        Collections.emptyList(),
        Collections.emptyList());
  }

  @Override
  public String getName() {
    return DASHBOARD_VAR_TABLE;
  }
}
