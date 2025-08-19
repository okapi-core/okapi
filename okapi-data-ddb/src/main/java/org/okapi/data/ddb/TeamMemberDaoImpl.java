package org.okapi.data.ddb;

import com.google.common.collect.Lists;
import java.util.List;
import org.okapi.data.dao.TeamMemberDao;
import org.okapi.data.ddb.iterators.FlatteningIterator;
import org.okapi.data.ddb.iterators.MappingIterator;
import org.okapi.data.dto.TablesAndIndexes;
import org.okapi.data.dto.TeamMemberDto;
import org.okapi.data.dto.TeamMemberDtoDdb;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;

public class TeamMemberDaoImpl implements TeamMemberDao {
  DynamoDbEnhancedClient enhancedClient;
  DynamoDbTable<TeamMemberDtoDdb> dynamoDbTable;

  public TeamMemberDaoImpl(DynamoDbEnhancedClient enhancedClient) {
    this.enhancedClient = enhancedClient;
    dynamoDbTable =
        enhancedClient.table(
            TablesAndIndexes.TEAM_MEMBERS_TABLE, TableSchema.fromBean(TeamMemberDtoDdb.class));
  }

  @Override
  public void addMember(String teamId, String userId) {
    var teamMember = TeamMemberDtoDdb.builder().teamId(teamId).userId(userId).build();
    dynamoDbTable.putItem(teamMember);
  }

  @Override
  public void removeMember(String teamId, String userId) {
    dynamoDbTable.deleteItem(Key.builder().partitionValue(teamId).sortValue(userId).build());
  }

  @Override
  public List<TeamMemberDto> listMembers(String teamId) {
    var query =
        dynamoDbTable.query(
            QueryEnhancedRequest.builder()
                .queryConditional(
                    QueryConditional.keyEqualTo(Key.builder().partitionValue(teamId).build()))
                .build());
    return Lists.newArrayList(
        new MappingIterator<>(new FlatteningIterator<>(query.iterator()), this::toDto));
  }

  public TeamMemberDto toDto(TeamMemberDtoDdb teamMemberDtoDdb) {
    if (teamMemberDtoDdb == null) return null;
    else
      return TeamMemberDto.builder()
          .teamId(teamMemberDtoDdb.getTeamId())
          .userId(teamMemberDtoDdb.getUserId())
          .build();
  }

  public TeamMemberDtoDdb fromDto(TeamMemberDto teamMemberDto) {
    if (teamMemberDto == null) return null;
    else
      return TeamMemberDtoDdb.builder()
          .teamId(teamMemberDto.getTeamId())
          .userId(teamMemberDto.getUserId())
          .build();
  }
}
