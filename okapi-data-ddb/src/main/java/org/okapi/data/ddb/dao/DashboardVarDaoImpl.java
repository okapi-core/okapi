/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.data.ddb.dao;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import java.util.List;
import java.util.Optional;
import org.okapi.data.dao.DashboardVarDao;
import org.okapi.data.ddb.iterators.FlatteningIterator;
import org.okapi.data.dto.DashboardVariable;
import org.okapi.data.dto.TablesAndIndexes;
import org.okapi.data.exceptions.ResourceNotFoundException;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;

public class DashboardVarDaoImpl implements DashboardVarDao {
  DynamoDbTable<DashboardVariable> table;

  @Inject
  public DashboardVarDaoImpl(DynamoDbEnhancedClient enhancedClient) {
    this.table =
        enhancedClient.table(
            TablesAndIndexes.DASHBOARD_VAR_TABLE, TableSchema.fromBean(DashboardVariable.class));
  }

  @Override
  public Optional<DashboardVariable> get(
      String orgId, String dashboardId, String versionId, String name) {
    var orgDashKey = DashboardVariable.orgDashKey(orgId, dashboardId, versionId);
    var found = table.getItem(Key.builder().partitionValue(orgDashKey).sortValue(name).build());
    return Optional.ofNullable(found);
  }

  @Override
  public DashboardVariable save(
      String orgId, String dashboardId, String versionId, DashboardVariable variable) {
    Preconditions.checkNotNull(variable);
    variable.setOrgDashKey(DashboardVariable.orgDashKey(orgId, dashboardId, versionId));
    table.putItem(variable);
    return variable;
  }

  @Override
  public void delete(String orgId, String dashboardId, String versionId, String name)
      throws ResourceNotFoundException {
    var orgDashKey = DashboardVariable.orgDashKey(orgId, dashboardId, versionId);
    table.deleteItem(Key.builder().partitionValue(orgDashKey).sortValue(name).build());
  }

  @Override
  public List<DashboardVariable> list(String orgId, String dashboardId, String versionId) {
    var orgDashKey = DashboardVariable.orgDashKey(orgId, dashboardId, versionId);
    var q =
        table.query(
            QueryEnhancedRequest.builder()
                .queryConditional(
                    QueryConditional.keyEqualTo(Key.builder().partitionValue(orgDashKey).build()))
                .build());
    return Lists.newArrayList(new FlatteningIterator<>(q.iterator()));
  }
}
