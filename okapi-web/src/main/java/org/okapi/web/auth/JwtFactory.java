/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.web.auth;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.springframework.stereotype.Service;

@Service
public class JwtFactory {
  public JWTCreator.Builder getShortTermBuilder() {
    return JWT.create().withExpiresAt(Instant.now().plus(1, ChronoUnit.DAYS));
  }

  public JWTCreator.Builder getLongTermBuilder() {
    return JWT.create().withExpiresAt(Instant.now().plus(3, ChronoUnit.MONTHS));
  }
}
