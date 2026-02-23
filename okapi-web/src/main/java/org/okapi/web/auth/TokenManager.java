package org.okapi.web.auth;

import static org.okapi.validation.OkapiChecks.checkArgument;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import lombok.AllArgsConstructor;
import org.okapi.exceptions.UnAuthorizedException;
import org.springframework.stereotype.Service;

@AllArgsConstructor
@Service
public class TokenManager {

  Algorithm algorithm;
  AccessManager accessManager;
  TokenDecoder decoder;
  JwtFactory jwtFactory;

  public String issueLoginToken(String userId) {
    return JWT.create()
        .withClaim(JwtClaims.CLAIM_USER_ID, userId)
        .withExpiresAt(Instant.now().plus(1, ChronoUnit.DAYS))
        .withClaim(JwtClaims.TOKEN_TYPE, TOKEN_TYPE.LOGIN.name())
        .sign(algorithm);
  }

  public String issueTemporaryToken(String loginToken, String orgId) throws UnAuthorizedException {
    var decoded = decoder.decode(loginToken, algorithm);
    var tokenType = decoded.getClaim(JwtClaims.TOKEN_TYPE);
    checkArgument(tokenType != null, UnAuthorizedException::new);
    checkClaims(loginToken, Map.of(JwtClaims.TOKEN_TYPE, TOKEN_TYPE.LOGIN.name()));
    var userId = decoded.getClaim(JwtClaims.CLAIM_USER_ID);
    if (userId == null) throw new UnAuthorizedException();
    accessManager.checkUserHasIsOrgMember(userId.asString(), orgId);
    return jwtFactory
        .getShortTermBuilder()
        .withClaim(JwtClaims.TOKEN_TYPE, TOKEN_TYPE.TEMP.name())
        .withClaim(JwtClaims.CLAIM_ORG_ID, orgId)
        .withClaim(JwtClaims.CLAIM_USER_ID, userId.asString())
        .sign(algorithm);
  }

  public void checkClaims(String token, Map<String, Object> claims) throws UnAuthorizedException {
    var decoded = decoder.decode(token, algorithm);
    for (var entry : claims.entrySet()) {
      var claim = decoded.getClaim(entry.getKey());
      if (claim == null || !claim.asString().equals(entry.getValue().toString())) {
        throw new UnAuthorizedException();
      }
    }
  }

  public String getUserId(String tempToken) throws UnAuthorizedException {
    return decoder.checkClaimOrThrow(tempToken, JwtClaims.CLAIM_USER_ID, algorithm);
  }

  public String getOrgId(String tempToken) throws UnAuthorizedException {
    return decoder.checkClaimOrThrow(tempToken, JwtClaims.CLAIM_ORG_ID, algorithm);
  }

  public String getTokenType(String token) throws UnAuthorizedException {
    return decoder.checkClaimOrThrow(token, JwtClaims.TOKEN_TYPE, algorithm);
  }

  public AccessManager.AuthContext getAuthContext(String tempToken) throws UnAuthorizedException {
    var userId = getUserId(tempToken);
    var orgId = getOrgId(tempToken);
    return new AccessManager.AuthContext(userId, orgId);
  }
}
