package org.okapi.data.ddb;

import com.google.common.base.Preconditions;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.okapi.data.dao.DashboardDao;
import org.okapi.data.dto.DashboardDdb;
import org.okapi.data.dto.DashboardDto;
import org.okapi.data.dto.TablesAndIndexes;
import org.okapi.data.exceptions.ResourceNotFoundException;
import org.okapi.data.s3.S3Prefixes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.enhanced.dynamodb.*;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

public class DashboardDaoImpl extends AbstractDdbDao<DashboardDdb, DashboardDto>
    implements DashboardDao {

  String bucket;
  S3Client s3;

  private static final TableSchema<DashboardDdb> SCHEMA = TableSchema.fromBean(DashboardDdb.class);

  public DashboardDaoImpl(
      DynamoDbEnhancedClient dynamoDbEnhancedClient, S3Client s3, String bucket) {
    super(TablesAndIndexes.DASHBOARDS_TABLE, dynamoDbEnhancedClient, DashboardDdb.class);
    this.s3 = s3;
    this.bucket = bucket;
  }

  @Override
  public DashboardDto save(DashboardDto dto) {
    Preconditions.checkNotNull(dto);
    var obj = fromDto(dto);
    table.putItem(obj);
    return dto;
  }

  @Override
  public void updateDefinition(String id, String definition) throws ResourceNotFoundException {
    var prefix = S3Prefixes.getDashboardDefinitionPrefix(id);
    s3.putObject(
        PutObjectRequest.builder().bucket(bucket).key(prefix).build(),
        RequestBody.fromString(definition));
  }

  @Override
  public String getDefinition(String id) throws ResourceNotFoundException {
    var prefix = S3Prefixes.getDashboardDefinitionPrefix(id);
    var getObj = GetObjectRequest.builder().bucket(bucket).key(prefix).build();
    var response = s3.getObjectAsBytes(getObj).asString(StandardCharsets.UTF_8);
    return response;
  }

  @Override
  public Optional<DashboardDto> get(String dashboardId) {
    DashboardDdb found = table.getItem(Key.builder().partitionValue(dashboardId).build());
    return Optional.ofNullable(toDto(found));
  }

  @Override
  public void updateNote(String id, String note) throws ResourceNotFoundException {
    var ddb = table.getItem(Key.builder().partitionValue(id).build());
    ddb.setDashboardNote(note);
    table.putItem(ddb);
  }

  @Override
  public DashboardDdb fromDto(DashboardDto dto) {
    if (dto == null) return null;
    return DashboardDdb.builder()
        .bucket(dto.getOrgId())
        .orgId(dto.getOrgId())
        .created(dto.getCreated())
        .updatedTime(dto.getUpdatedTime())
        .dashboardNote(dto.getDashboardNote())
        .dashboardStatus(dto.getDashboardStatus())
        .dashboardTitle(dto.getDashboardTitle())
        .creator(dto.getCreator())
        .dashboardId(dto.getDashboardId())
        .lastEditor(dto.getLastEditor())
        .prefix(dto.getPrefix())
        .version(dto.getVersion())
        .build();
  }

  @Override
  public DashboardDto toDto(DashboardDdb obj) {
    if (obj == null) return null;
    return DashboardDto.builder()
        .bucket(obj.getOrgId())
        .orgId(obj.getOrgId())
        .created(obj.getCreated())
        .updatedTime(obj.getUpdatedTime())
        .dashboardNote(obj.getDashboardNote())
        .dashboardStatus(obj.getDashboardStatus())
        .dashboardTitle(obj.getDashboardTitle())
        .creator(obj.getCreator())
        .dashboardId(obj.getDashboardId())
        .lastEditor(obj.getLastEditor())
        .prefix(obj.getPrefix())
        .version(obj.getVersion())
        .build();
  }
}
