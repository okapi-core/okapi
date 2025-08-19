package org.okapi.data.ddb;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import org.okapi.data.dao.TeamsDao;
import org.okapi.data.ddb.iterators.FlatteningIterator;
import org.okapi.data.ddb.iterators.MappingIterator;
import org.okapi.data.dto.TablesAndIndexes;
import org.okapi.data.dto.TeamDto;
import org.okapi.data.dto.TeamDtoDdb;
import org.okapi.data.exceptions.TeamNotFoundException;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.model.GetItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;

import java.util.List;
import java.util.Optional;

public class TeamsDaoImpl extends AbstractDdbDao<TeamDtoDdb, TeamDto> implements TeamsDao {

  public TeamsDaoImpl(DynamoDbEnhancedClient dynamoDbEnhancedClient) {
    super(TablesAndIndexes.TEAMS_TABLE, dynamoDbEnhancedClient, TeamDtoDdb.class);
  }

  @Override
  public TeamDto create(TeamDto dto) {
    Preconditions.checkNotNull(dto);
    var obj = fromDto(dto);
    table.putItem(obj);
    return dto;
  }

  @Override
  public Optional<TeamDto> get(String teamId) {
    Preconditions.checkNotNull(teamId);
    var obj =
        table.getItem(
            GetItemEnhancedRequest.builder()
                .key(Key.builder().partitionValue(teamId).build())
                .build());
    return Optional.ofNullable(toDto(obj));
  }

  @Override
  public List<TeamDto> listByOrgId(String orgId) {
    var idx = table.index(TablesAndIndexes.ORG_TO_TEAM_GSI);
    var res =
        idx.query(
            QueryEnhancedRequest.builder()
                .queryConditional(
                    QueryConditional.keyEqualTo(Key.builder().partitionValue(orgId).build()))
                .build());
    return Lists.newArrayList(
        new MappingIterator<>(new FlatteningIterator<>(res.iterator()), this::toDto));
  }

  @Override
  public void delete(String teamId) {
    table.deleteItem(Key.builder().partitionValue(teamId).build());
  }

  @Override
  public TeamDto update(String teamId, String teamName, String description)
      throws TeamNotFoundException {
    var dto = get(teamId);
    if (dto.isEmpty()) throw new TeamNotFoundException();
    var obj = fromDto(dto.get());
    obj.setTeamName(teamName);
    obj.setDescription(description);
    table.putItem(obj);
    return toDto(obj);
  }

  @Override
  public TeamDtoDdb fromDto(TeamDto dto) {
    if (dto == null) return null;
    return TeamDtoDdb.builder()
        .teamId(dto.getTeamId())
        .createdAt(dto.getCreatedAt())
        .description(dto.getDescription())
        .orgId(dto.getOrgId())
        .teamName(dto.getTeamName())
        .build();
  }

  @Override
  public TeamDto toDto(TeamDtoDdb obj) {
    if (obj == null) return null;
    return TeamDto.builder()
        .teamId(obj.getTeamId())
        .createdAt(obj.getCreatedAt())
        .description(obj.getDescription())
        .orgId(obj.getOrgId())
        .teamName(obj.getTeamName())
        .build();
  }
}
