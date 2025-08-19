package org.okapi.fixtures;

import com.auth0.jwt.algorithms.Algorithm;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.function.Supplier;
import org.okapi.auth.*;
import org.okapi.data.dao.*;
import org.okapi.tokens.AuthorizationTokenService;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;

public class SingletonFactory {
  Map<String, Object> singletons = new HashMap<>();

  public <T> T makeSingleton(Class<T> clazz, Supplier<T> objSupplier) {
    var key = "/" + clazz.getSimpleName();
    if (singletons.containsKey(key)) {
      return (T) singletons.get(key);
    } else {
      singletons.put(key, objSupplier.get());
      return (T) singletons.get(key);
    }
  }

  public AwsCredentialsProvider credentialsProvider() {
    return makeSingleton(
        AwsCredentialsProvider.class, EnvironmentVariableCredentialsProvider::create);
  }

  public DaoFactory daoFactory() {
    return makeSingleton(
        DaoFactory.class, () -> ServiceLoader.load(DaoFactory.class).findFirst().get());
  }

  public TeamsDao teamsDao() {
    return makeSingleton(TeamsDao.class, () -> daoFactory().teamsDao());
  }

  public TeamMemberDao teamMemberDao() {
    return makeSingleton(TeamMemberDao.class, () -> daoFactory().teamMemberDao());
  }

  public RelationGraphDao relationGraphDao() {
    return makeSingleton(RelationGraphDao.class, () -> daoFactory().relationGraphDao());
  }

  public TeamsManager teamsManager() {
    return makeSingleton(
        TeamsManager.class,
        () ->
            new TeamsManager(
                accessManager(),
                tokenManager(),
                usersDao(),
                teamsDao(),
                teamMemberDao(),
                relationGraphDao()));
  }

  public UsersDao usersDao() {
    return makeSingleton(UsersDao.class, () -> daoFactory().usersDao());
  }


  public Algorithm algorithm() {
    var secret = "TestSecretDoNotUseInProd";
    return makeSingleton(Algorithm.class, () -> Algorithm.HMAC256(secret));
  }

  public AccessManager accessManager() {
    return makeSingleton(AccessManager.class, () -> new AccessManager(usersDao()));
  }

  public OrgDao orgDao() {
    return makeSingleton(OrgDao.class, () -> daoFactory().orgDao());
  }

  public UserManager getUserManager() {
    return makeSingleton(
        UserManager.class,
        () ->
            new UserManager(
                usersDao(),
                orgDao(),
                tokenManager(),
                teamsManager()));
  }

  public AuthorizationTokenDao authorizationTokenDao() {
    return makeSingleton(AuthorizationTokenDao.class, () -> daoFactory().authorizationTokenDao());
  }

  public AuthorizationTokenService authorizationTokenService() {
    return makeSingleton(
        AuthorizationTokenService.class,
        () ->
            new AuthorizationTokenService(
                authorizationTokenDao(), teamsDao(), orgDao(), accessManager(), tokenManager()));
  }

  public TokenManager tokenManager() {
    return makeSingleton(TokenManager.class, () -> new TokenManager(algorithm(), accessManager()));
  }

  public OrgManager orgManager() {
    return makeSingleton(
        OrgManager.class,
        () ->
            new OrgManager(
                tokenManager(), accessManager(), usersDao(), orgDao(), relationGraphDao()));
  }
}
