package org.okapi.data.ddb.dao;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import java.util.List;
import java.util.Optional;
import org.okapi.data.dao.DashboardDao;
import org.okapi.data.ddb.iterators.FlatteningIterator;
import org.okapi.data.dto.DashboardDdb;
import org.okapi.data.dto.TablesAndIndexes;
import org.okapi.data.exceptions.ResourceNotFoundException;
import software.amazon.awssdk.enhanced.dynamodb.*;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;

public class DashboardDaoImpl implements DashboardDao {

  DynamoDbTable<DashboardDdb> table;

  @Inject
  public DashboardDaoImpl(DynamoDbEnhancedClient enhancedClient) {
   
    this.table =
        enhancedClient.table(
            TablesAndIndexes.DASHBOARDS_TABLE, TableSchema.fromBean(DashboardDdb.class));
  }

  @Override
  public DashboardDdb save(DashboardDdb dto) {
    Preconditions.checkNotNull(dto);
    table.putItem(dto);
    return dto;
  }

  @Override
  public void delete(String id) throws ResourceNotFoundException {
    // find item by sort key (dashboardId) and delete with full key
    var scan = table.scan();
    DashboardDdb found = null;
    for (DashboardDdb item : scan.items()) {
      if (id.equals(item.getDashboardId())) {
        found = item;
        break;
      }
    }
    if (found == null) {
      throw new ResourceNotFoundException("Dashboard with id " + id + " not found");
    }
    table.deleteItem(
        Key.builder().partitionValue(found.getOrgId()).sortValue(found.getDashboardId()).build());
  }

  @Override
  public List<DashboardDdb> getAll(String orgId) {
    var query =
        table.query(
            QueryEnhancedRequest.builder()
                .queryConditional(
                    QueryConditional.keyEqualTo(Key.builder().partitionValue(orgId).build()))
                .build());
    return Lists.newArrayList(new FlatteningIterator<>(query.iterator()));
  }

  @Override
  public Optional<DashboardDdb> get(String orgId, String dashboardId) {
    DashboardDdb found =
        table.getItem(Key.builder().partitionValue(orgId).sortValue(dashboardId).build());
    return Optional.ofNullable(found);
  }
}
