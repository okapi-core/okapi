package org.okapi.metricsproxy.auth;

import static org.okapi.validation.OkapiChecks.checkArgument;

import lombok.AllArgsConstructor;
import org.okapi.data.dao.AuthTokenCache;
import org.okapi.data.dto.AuthorizationTokenDto;
import org.okapi.exceptions.UnAuthorizedException;

@AllArgsConstructor
public class AuthorizationChecker {

  AuthTokenCache tokenCache;

  public String resolveToken(String header) throws UnAuthorizedException {
    var split = header.split(" ");
    checkArgument(split.length == 2, UnAuthorizedException::new);
    checkArgument(split[0].equals("Bearer"), UnAuthorizedException::new);

    var maybeToken = tokenCache.get(split[1]);
    checkArgument(maybeToken.isPresent(), UnAuthorizedException::new);
    var token = maybeToken.get();
    checkArgument(
            token.getTokenStatus() != AuthorizationTokenDto.AuthorizationTokenStatus.INACTIVE,
            UnAuthorizedException::new);
    checkArgument(isNotExpired(token), UnAuthorizedException::new);
    return token.getTeamId();
  }

  public AuthorizationTokenDto resolve(String header) throws UnAuthorizedException {
    var split = header.split(" ");
    checkArgument(split.length == 2, UnAuthorizedException::new);
    checkArgument(split[0].equals("Bearer"), UnAuthorizedException::new);

    var maybeToken = tokenCache.get(split[1]);
    checkArgument(maybeToken.isPresent(), UnAuthorizedException::new);

    var token = maybeToken.get();
    checkArgument(
        token.getTokenStatus() != AuthorizationTokenDto.AuthorizationTokenStatus.INACTIVE,
        UnAuthorizedException::new);
    checkArgument(isNotExpired(token), UnAuthorizedException::new);
    return token;
  }

  private boolean isNotExpired(AuthorizationTokenDto tokenDto) {
    return tokenDto.getExpiryTime() > System.currentTimeMillis();
  }
}
