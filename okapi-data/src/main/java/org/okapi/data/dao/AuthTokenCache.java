package org.okapi.data.dao;

import org.okapi.data.dto.AuthorizationTokenDto;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class AuthTokenCache {

  Map<String, CachedData<AuthorizationTokenDto>> cache;
  Duration lifeTime;
  AuthorizationTokenDao tokenDao;

  public AuthTokenCache(Duration lifeTime, AuthorizationTokenDao tokenDao) {
    this.lifeTime = lifeTime;
    this.tokenDao = tokenDao;
    cache = new HashMap<>();
  }

  public Optional<AuthorizationTokenDto> get(String id) {
    fetchAndCache(id);
    var maybeData = cache.get(id);
    if (maybeData == null) {
      return Optional.empty();
    } else {
      return Optional.ofNullable(maybeData.getData());
    }
  }

  public void fetchAndCache(String id) {
    var cached = cache.get(id);
    if (cached == null) {
      fetchFromRemote(id);
    }
    else if (System.currentTimeMillis() - cached.getCacheTime() > lifeTime.toMillis()) {
      fetchFromRemote(id);
    }
  }

  public void fetchFromRemote(String id) {
    var token = tokenDao.findToken(id);
    if (token.isEmpty()) {
      return;
    }
    cache.put(id, new CachedData<>(System.currentTimeMillis(), token.get()));
  }
}
