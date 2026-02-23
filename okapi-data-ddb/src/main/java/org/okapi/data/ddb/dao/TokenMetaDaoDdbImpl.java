package org.okapi.data.ddb.dao;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import java.util.List;
import org.okapi.data.dao.TokenMetaDao;
import org.okapi.data.ddb.iterators.FlatteningIterator;
import org.okapi.data.dto.TOKEN_STATUS;
import org.okapi.data.dto.TablesAndIndexes;
import org.okapi.data.dto.TokenMetaDdb;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;

public class TokenMetaDaoDdbImpl implements TokenMetaDao {

  private final DynamoDbEnhancedClient dynamoDbEnhancedClient;
  private final DynamoDbTable<TokenMetaDdb> tokenMetaTable;

  @Inject
  public TokenMetaDaoDdbImpl(DynamoDbEnhancedClient dynamoDbEnhancedClient) {
    this.dynamoDbEnhancedClient = dynamoDbEnhancedClient;
    this.tokenMetaTable =
        this.dynamoDbEnhancedClient.table(
            TablesAndIndexes.TOKEN_META_TABLE, TableSchema.fromBean(TokenMetaDdb.class));
  }

  @Override
  public void createTokenMetadata(TokenMetaDdb tokenMeta) {
    tokenMetaTable.putItem(tokenMeta);
  }

  @Override
  public TokenMetaDdb getTokenMetadata(String orgId, String tokenId) {
    return tokenMetaTable.getItem(
        Key.builder().partitionValue(orgId).sortValue(tokenId).build());
  }

  @Override
  public void updateTokenStatus(String orgId, String tokenId, TOKEN_STATUS status) {
    var token = getTokenMetadata(orgId, tokenId);
    if (token == null) {
      return;
    }
    token.setTokenStatus(status);
    tokenMetaTable.updateItem(token);
  }

  @Override
  public List<TokenMetaDdb> listTokensByOrgAndStatus(String orgId, TOKEN_STATUS status) {
    var index = tokenMetaTable.index(TablesAndIndexes.TOKEN_META_BY_ORG_STATUS_GSI);
    var results =
        index.query(
            QueryConditional.keyEqualTo(
                Key.builder().partitionValue(orgId).sortValue(status.name()).build()));
    return Lists.newArrayList(new FlatteningIterator<>(results.iterator()));
  }
}
