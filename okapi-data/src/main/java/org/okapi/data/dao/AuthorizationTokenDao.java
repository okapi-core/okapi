package org.okapi.data.dao;

import org.okapi.data.dto.AuthorizationTokenDto;
import java.util.Optional;
import java.util.Iterator;

public interface AuthorizationTokenDao {
  AuthorizationTokenDto putToken(AuthorizationTokenDto authorizationToken);

  Optional<AuthorizationTokenDto> findToken(String authorizationToken);

  Iterator<AuthorizationTokenDto> listTokenByTeam(String teamId);

  void deleteToken(String authorizationToken);
}
