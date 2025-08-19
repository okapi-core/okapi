package org.okapi.data.ddb;

import static org.okapi.data.dto.TablesAndIndexes.AUTHORIZATION_TOKENS_TABLE;
import static org.okapi.data.dto.TablesAndIndexes.TEAM_TO_AUTHORIZATION_TOKEN_GSI;

import java.util.Iterator;
import java.util.Optional;
import org.okapi.data.dao.AuthorizationTokenDao;
import org.okapi.data.ddb.iterators.FlatteningIterator;
import org.okapi.data.ddb.iterators.MappingIterator;
import org.okapi.data.dto.AuthorizationTokenDdb;
import org.okapi.data.dto.AuthorizationTokenDto;
import software.amazon.awssdk.enhanced.dynamodb.*;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbIndex;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;

public class AuthorizationTokenDaoDdbImpl
    extends AbstractDdbDao<AuthorizationTokenDdb, AuthorizationTokenDto>
    implements AuthorizationTokenDao {

  public AuthorizationTokenDaoDdbImpl(DynamoDbEnhancedClient dynamoDbEnhancedClient) {
    super(AUTHORIZATION_TOKENS_TABLE, dynamoDbEnhancedClient, AuthorizationTokenDdb.class);
  }

  @Override
  public AuthorizationTokenDto putToken(AuthorizationTokenDto authorizationToken) {
    AuthorizationTokenDdb item = fromDto(authorizationToken);
    table.putItem(item);
    return authorizationToken;
  }

  @Override
  public Optional<AuthorizationTokenDto> findToken(String authorizationToken) {
    AuthorizationTokenDdb found =
        table.getItem(Key.builder().partitionValue(authorizationToken).build());
    return Optional.ofNullable(toDto(found));
  }

  @Override
  public Iterator<AuthorizationTokenDto> listTokenByTeam(String teamId) {
    DynamoDbIndex<AuthorizationTokenDdb> gsi = table.index(TEAM_TO_AUTHORIZATION_TOKEN_GSI);
    var results =
        gsi.query(
            QueryEnhancedRequest.builder()
                .queryConditional(
                    QueryConditional.keyEqualTo(Key.builder().partitionValue(teamId).build()))
                .build());
    return new MappingIterator<>(new FlatteningIterator<>(results.iterator()), this::toDto);
  }

  @Override
  public void deleteToken(String authorizationToken) {
    table.deleteItem(Key.builder().partitionValue(authorizationToken).build());
  }

  @Override
  public AuthorizationTokenDdb fromDto(AuthorizationTokenDto dto) {
    if (dto == null) return null;
    return AuthorizationTokenDdb.builder()
        .orgId(dto.getOrgId())
        .authorizationRoles(dto.getAuthorizationRoles())
        .authorizationToken(dto.getAuthorizationToken())
        .created(dto.getCreated())
        .tokenStatus(dto.getTokenStatus())
        .expiryTime(dto.getExpiryTime())
        .issuer(dto.getIssuer())
        .teamId(dto.getTeamId())
        .build();
  }

  @Override
  public AuthorizationTokenDto toDto(AuthorizationTokenDdb obj) {
    if (obj == null) return null;
    return AuthorizationTokenDto.builder()
        .orgId(obj.getOrgId())
        .authorizationRoles(obj.getAuthorizationRoles())
        .authorizationToken(obj.getAuthorizationToken())
        .created(obj.getCreated())
        .tokenStatus(obj.getTokenStatus())
        .expiryTime(obj.getExpiryTime())
        .issuer(obj.getIssuer())
        .teamId(obj.getTeamId())
        .build();
  }
}
