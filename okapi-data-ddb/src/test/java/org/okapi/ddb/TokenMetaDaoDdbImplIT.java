package org.okapi.ddb;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.okapi.data.CreateDynamoDBTables;
import org.okapi.data.dao.TokenMetaDao;
import org.okapi.data.ddb.dao.TokenMetaDaoDdbImpl;
import org.okapi.data.dto.TOKEN_STATUS;
import org.okapi.data.dto.TokenMetaDdb;
import org.okapi.testutils.OkapiTestUtils;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;

public class TokenMetaDaoDdbImplIT {

  private TokenMetaDao dao;
  private String orgId;

  @BeforeEach
  public void setup() {
    CreateDynamoDBTables.createTables(OkapiTestUtils.getLocalStackDynamoDbClient());
    var enhanced =
        DynamoDbEnhancedClient.builder()
            .dynamoDbClient(OkapiTestUtils.getLocalStackDynamoDbClient())
            .build();
    dao = new TokenMetaDaoDdbImpl(enhanced);
    orgId = OkapiTestUtils.getTestId(getClass()) + ":" + UUID.randomUUID();
  }

  private TokenMetaDdb newToken(String tokenId, TOKEN_STATUS status) {
    var meta = new TokenMetaDdb();
    meta.setOrgId(orgId);
    meta.setTokenId(tokenId);
    meta.setTokenStatus(status);
    meta.setCreatedAt(System.currentTimeMillis());
    meta.setCreatorId("creator-" + tokenId);
    return meta;
  }

  @Test
  public void createAndGetTokenMetadata() {
    var tokenId = "t-" + UUID.randomUUID();
    var meta = newToken(tokenId, TOKEN_STATUS.ACTIVE);

    dao.createTokenMetadata(meta);

    var stored = dao.getTokenMetadata(orgId, tokenId);
    assertNotNull(stored);
    assertEquals(orgId, stored.getOrgId());
    assertEquals(tokenId, stored.getTokenId());
    assertEquals(TOKEN_STATUS.ACTIVE, stored.getTokenStatus());
    assertEquals("creator-" + tokenId, stored.getCreatorId());
  }

  @Test
  public void updateTokenStatusChangesStatus() {
    var tokenId = "update-" + UUID.randomUUID();
    dao.createTokenMetadata(newToken(tokenId, TOKEN_STATUS.ACTIVE));

    dao.updateTokenStatus(orgId, tokenId, TOKEN_STATUS.INACTIVE);

    var stored = dao.getTokenMetadata(orgId, tokenId);
    assertNotNull(stored);
    assertEquals(TOKEN_STATUS.INACTIVE, stored.getTokenStatus());
  }

  @Test
  public void listTokensByOrgAndStatusFiltersCorrectly() {
    var activeTokens = List.of("a1-" + UUID.randomUUID(), "a2-" + UUID.randomUUID());
    var inactiveTokens = List.of("i1-" + UUID.randomUUID());
    for (var id : activeTokens) {
      dao.createTokenMetadata(newToken(id, TOKEN_STATUS.ACTIVE));
    }
    for (var id : inactiveTokens) {
      dao.createTokenMetadata(newToken(id, TOKEN_STATUS.INACTIVE));
    }

    // add another org to ensure isolation
    var other = new TokenMetaDdb();
    other.setOrgId("other-org");
    other.setTokenId("other-token");
    other.setTokenStatus(TOKEN_STATUS.ACTIVE);
    dao.createTokenMetadata(other);

    var activeResult = dao.listTokensByOrgAndStatus(orgId, TOKEN_STATUS.ACTIVE);
    var ids =
        activeResult.stream().map(TokenMetaDdb::getTokenId).toList();
    assertTrue(ids.containsAll(activeTokens));
    assertFalse(ids.containsAll(inactiveTokens));
    assertFalse(ids.contains("other-token"));

    var inactiveResult = dao.listTokensByOrgAndStatus(orgId, TOKEN_STATUS.INACTIVE);
    var inactiveIds = inactiveResult.stream().map(TokenMetaDdb::getTokenId).toList();
    assertTrue(inactiveIds.containsAll(inactiveTokens));
    assertTrue(inactiveIds.stream().noneMatch(activeTokens::contains));
  }
}
