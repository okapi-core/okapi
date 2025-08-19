package org.okapi.data.factory;

import com.google.auto.service.AutoService;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import org.okapi.data.dao.*;
import org.okapi.data.ddb.*;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

@AutoService(DaoFactory.class)
public class ResourceFactory implements DaoFactory {

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

  public S3Client s3Client() {
    return makeSingleton(
        S3Client.class,
        () -> {
          return S3Client.builder()
              .region(Region.US_EAST_1) // LocalStack ignores region but SDK requires it
              .endpointOverride(URI.create("http://localhost:4566")) // LocalStack endpoint
              .credentialsProvider(
                  StaticCredentialsProvider.create(
                      AwsBasicCredentials.create("test", "test") // Dummy creds for LocalStack
                      ))
              .serviceConfiguration(
                  S3Configuration.builder()
                      .pathStyleAccessEnabled(true) // Needed for LocalStack S3
                      .build())
              .build();
        });
  }

  public DynamoDbClient dynamoDB() {
    return makeSingleton(
        DynamoDbClient.class,
        () ->
            DynamoDbClient.builder()
                .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                .endpointOverride(URI.create("http://localhost:4566"))
                .region(Region.of("eu-west-2"))
                .build());
  }

  public DynamoDbEnhancedClient enhancedClient() {
    return makeSingleton(
        DynamoDbEnhancedClient.class,
        () -> DynamoDbEnhancedClient.builder().dynamoDbClient(dynamoDB()).build());
  }

  @Override
  public AuthorizationTokenDao authorizationTokenDao() {
    return makeSingleton(
        AuthorizationTokenDaoDdbImpl.class,
        () -> new AuthorizationTokenDaoDdbImpl(enhancedClient()));
  }

  @Override
  public DashboardDao dashboardDao() {
    return makeSingleton(
        DashboardDaoImpl.class,
        () -> new DashboardDaoImpl(enhancedClient(), s3Client(), "test-bucket"));
  }

  @Override
  public OrgDao orgDao() {
    return makeSingleton(OrgDaoDdbImpl.class, () -> new OrgDaoDdbImpl(enhancedClient()));
  }

  public RelationGraphDaoImpl relationGraphDao() {
    return makeSingleton(
        RelationGraphDaoImpl.class, () -> new RelationGraphDaoImpl(enhancedClient()));
  }

  @Override
  public TeamMemberDao teamMemberDao() {
    return makeSingleton(TeamMemberDaoImpl.class, () -> new TeamMemberDaoImpl(enhancedClient()));
  }

  @Override
  public TeamsDao teamsDao() {
    return makeSingleton(TeamsDaoImpl.class, () -> new TeamsDaoImpl(enhancedClient()));
  }

  public UsersDaoImpl usersDao() {
    return makeSingleton(UsersDaoImpl.class, () -> new UsersDaoImpl(enhancedClient()));
  }
}
