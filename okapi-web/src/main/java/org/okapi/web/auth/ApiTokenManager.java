/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.web.auth;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class ApiTokenManager {
  Algorithm algorithm;
  TokenDecoder decoder;

  public String createApiToken(String orgId, List<String> claims) {
    return JWT.create()
        .withClaim(JwtClaims.CLAIM_ORG_ID, orgId)
        .withExpiresAt(Instant.now().plus(90, ChronoUnit.DAYS))
        .withClaim(JwtClaims.TOKEN_TYPE, TOKEN_TYPE.BEARER.name())
        .withClaim(JwtClaims.API_PERMISSIONS, claims)
        .sign(algorithm);
  }

  public AuthorizedEntity authorize(String token) {
    var decoded = decoder.decode(token, algorithm);
    var orgId = decoded.getClaim(JwtClaims.CLAIM_ORG_ID).asString();
    var permissions = decoded.getClaim(JwtClaims.API_PERMISSIONS).asList(String.class);
    return new AuthorizedEntity(orgId, permissions);
  }
}
