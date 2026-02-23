package org.okapi.data.dao;

import java.util.List;
import org.okapi.data.dto.TOKEN_STATUS;
import org.okapi.data.dto.TokenMetaDdb;

public interface TokenMetaDao {
  void createTokenMetadata(TokenMetaDdb tokenMeta);

  TokenMetaDdb getTokenMetadata(String orgId, String tokenId);

  void updateTokenStatus(String orgId, String tokenId, TOKEN_STATUS status);

  List<TokenMetaDdb> listTokensByOrgAndStatus(String orgId, TOKEN_STATUS status);
}
