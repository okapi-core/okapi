package org.okapi.data.ddb;

import static org.okapi.data.dto.TablesAndIndexes.ORGS_TABLE;

import java.util.Optional;
import org.okapi.data.dao.OrgDao;
import org.okapi.data.dto.OrgDto;
import org.okapi.data.dto.OrgDtoDdb;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.GetItemEnhancedRequest;

public class OrgDaoDdbImpl implements OrgDao {
  private final DynamoDbEnhancedClient enhanced;
  DynamoDbTable<OrgDtoDdb> dynamoDbTable;

  public OrgDaoDdbImpl(DynamoDbEnhancedClient enhanced) {
    this.enhanced = enhanced;
    dynamoDbTable = this.enhanced.table(ORGS_TABLE, TableSchema.fromBean(OrgDtoDdb.class));
  }

  @Override
  public Optional<OrgDto> findById(String orgId) {
    var obj =
        dynamoDbTable.getItem(
            GetItemEnhancedRequest.builder()
                .key(Key.builder().partitionValue(orgId).build())
                .build());
    return Optional.ofNullable(toDto(obj));
  }

  @Override
  public void save(OrgDto orgDto) {
    var obj = fromDto(orgDto);
    dynamoDbTable.putItem(obj);
  }

  private final OrgDto toDto(OrgDtoDdb orgDtoDdb) {
    if (orgDtoDdb == null) return null;
    return OrgDto.builder()
        .orgId(orgDtoDdb.getOrgId())
        .orgCreator(orgDtoDdb.getOrgCreator())
        .orgName(orgDtoDdb.getOrgName())
        .orgId(orgDtoDdb.getOrgId())
        .build();
  }

  private final OrgDtoDdb fromDto(OrgDto orgDto) {
    if (orgDto == null) return null;
    return OrgDtoDdb.builder()
        .orgCreator(orgDto.getOrgCreator())
        .orgName(orgDto.getOrgName())
        .orgId(orgDto.getOrgId())
        .build();
  }
}
