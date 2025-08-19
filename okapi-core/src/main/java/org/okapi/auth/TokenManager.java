package org.okapi.auth;


import static org.okapi.validation.OkapiChecks.checkArgument;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.okapi.exceptions.UnAuthorizedException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class TokenManager {

  Algorithm algorithm;
  AccessManager accessManager;

  public String issueLoginToken(String userId) {
    return JWT.create()
        .withClaim(JwtClaims.CLAIM_USER_ID, userId)
        .withExpiresAt(Instant.now().plus(1, ChronoUnit.DAYS))
        .withClaim(JwtClaims.TOKEN_TYPE, JwtClaims.LOGIN_TOKEN_TYPE)
        .sign(algorithm);
  }

  public String checkClaimOrThrow(String token, String claimName) throws UnAuthorizedException {
    try {
      var decoded = decode(token);
      var claim = decoded.getClaim(claimName);
      if (claim == null || claim.asString() == null) {
        throw new UnAuthorizedException();
      }
      return claim.asString();
    } catch (JWTDecodeException jwt) {
      throw new UnAuthorizedException();
    }
  }

  public String issueTemporaryBearerToken(String orgId, String loginToken)
      throws UnAuthorizedException {
    var decoded = decode(loginToken);
    var tokenType = decoded.getClaim(JwtClaims.TOKEN_TYPE);
    checkArgument(tokenType != null, UnAuthorizedException::new);
    checkClaims(loginToken, Map.of(JwtClaims.TOKEN_TYPE, JwtClaims.LOGIN_TOKEN_TYPE));
    var userId = decoded.getClaim(JwtClaims.CLAIM_USER_ID);
    if (userId == null) throw new UnAuthorizedException();
    accessManager.checkUserHasIsOrgMember(userId.asString(), orgId);
    return getJwtBuilder()
        .withClaim(JwtClaims.TOKEN_TYPE, JwtClaims.BEARER_TOKEN_TYPE)
        .withClaim(JwtClaims.CLAIM_ORG_ID, orgId)
        .withClaim(JwtClaims.CLAIM_USER_ID, userId.asString())
        .sign(algorithm);
  }

  public String generateAuthorizationToken() {
    return generateSecureRandomToken(256);
  }

  public void checkClaims(String token, Map<String, Object> claims) throws UnAuthorizedException {
    var decoded = decode(token);
    for (var entry : claims.entrySet()) {
      var claim = decoded.getClaim(entry.getKey());
      if (claim == null || !claim.asString().equals(entry.getValue().toString())) {
        throw new UnAuthorizedException();
      }
    }
  }

  private JWTCreator.Builder getJwtBuilder() {
    return JWT.create().withExpiresAt(Instant.now().plus(1, ChronoUnit.DAYS));
  }

  public String getUserId(String tempToken) throws UnAuthorizedException {
    return checkClaimOrThrow(tempToken, JwtClaims.CLAIM_USER_ID);
  }

  public String getOrgId(String tempToken) throws UnAuthorizedException {
    return checkClaimOrThrow(tempToken, JwtClaims.CLAIM_ORG_ID);
  }

  public String generateSecureRandomToken(int bits) {
    var bytes = new byte[bits / 8];
    SecureRandom random = new java.security.SecureRandom();
    random.nextBytes(bytes);
    StringBuilder token = new StringBuilder();
    for (byte b : bytes) {
      token.append(String.format("%02x", b));
    }
    return token.toString();
  }

  public DecodedJWT decode(String token) throws UnAuthorizedException {
    try {
      var verifier = JWT.require(algorithm).build();
      var decoded = verifier.verify(token);
      if (decoded == null) {
        throw new UnAuthorizedException("Token is invalid or expired");
      }
      return decoded;
    } catch (JWTDecodeException e) {
      throw new UnAuthorizedException("Invalid token format");
    }
  }
}
