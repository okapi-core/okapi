package org.okapi.data.ddb.dao;

import static org.okapi.data.dto.TablesAndIndexes.ORGS_TABLE;

import com.google.inject.Inject;
import java.util.Optional;
import org.okapi.data.dao.OrgDao;
import org.okapi.data.dto.OrgDtoDdb;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.GetItemEnhancedRequest;

public class OrgDaoDdbImpl implements OrgDao {
  private final DynamoDbEnhancedClient enhanced;
  DynamoDbTable<OrgDtoDdb> dynamoDbTable;

  @Inject
  public OrgDaoDdbImpl(DynamoDbEnhancedClient enhanced) {
    this.enhanced = enhanced;
    dynamoDbTable = this.enhanced.table(ORGS_TABLE, TableSchema.fromBean(OrgDtoDdb.class));
  }

  @Override
  public Optional<OrgDtoDdb> findById(String orgId) {
    var obj =
        dynamoDbTable.getItem(
            GetItemEnhancedRequest.builder()
                .key(Key.builder().partitionValue(orgId).build())
                .build());
    return Optional.ofNullable(toDto(obj));
  }

  @Override
  public void save(OrgDtoDdb orgDto) {
    var obj = fromDto(orgDto);
    dynamoDbTable.putItem(obj);
  }

  private final OrgDtoDdb toDto(OrgDtoDdb orgDtoDdb) {
    return orgDtoDdb;
  }

  private final OrgDtoDdb fromDto(OrgDtoDdb orgDto) {
    return orgDto;
  }
}
