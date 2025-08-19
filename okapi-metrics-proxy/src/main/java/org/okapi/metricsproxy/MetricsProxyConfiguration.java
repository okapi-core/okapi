package org.okapi.metricsproxy;

import com.auth0.jwt.algorithms.Algorithm;
import com.google.gson.Gson;
import java.net.URI;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ServiceLoader;
import java.util.UUID;
import okhttp3.OkHttpClient;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.leader.LeaderLatch;
import org.apache.curator.framework.recipes.locks.InterProcessLock;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.okapi.auth.*;
import org.okapi.beans.BeanIds;
import org.okapi.clock.Clock;
import org.okapi.clock.SystemClock;
import org.okapi.data.dao.*;
import org.okapi.fake.FakeClock;
import org.okapi.metrics.common.*;
import org.okapi.metrics.common.pojo.Node;
import org.okapi.metrics.common.pojo.NodeState;
import org.okapi.metrics.common.sharding.ConsistentHashedAssignerFactory;
import org.okapi.metrics.common.sharding.ShardsAndSeriesAssignerFactory;
import org.okapi.metricsproxy.auth.AuthorizationChecker;
import org.okapi.metricsproxy.service.*;
import org.okapi.profiles.ENV_TYPE;
import org.okapi.s3.S3ByteRangeCache;
import org.okapi.s3.S3Enhanced;
import org.okapi.secrets.SecretsManager;
import org.okapi.secrets.SecretsManagerImpl;
import org.okapi.tokens.AuthorizationTokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.auth.credentials.InstanceProfileCredentialsProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;

@Configuration
public class MetricsProxyConfiguration {

  @Autowired Environment environment;

  @Bean
  public ENV_TYPE env() {
    var profiles = environment.getActiveProfiles()[0];
    return ENV_TYPE.parse(profiles);
  }

  @Bean
  public Algorithm alg(@Autowired SecretsManager secretsManager) {
    var secret = secretsManager.getHmacKey();
    return Algorithm.HMAC256(secret);
  }

  @Bean
  public SecretsManagerClient secretsManagerClient(
      @Autowired ENV_TYPE envType, @Autowired AwsCredentialsProvider credentialsProvider) {
    return switch (envType) {
      case TEST, INTEG_TEST, ISO ->
          SecretsManagerClient.builder()
              .endpointOverride(URI.create("http://localhost:4566"))
              .credentialsProvider(credentialsProvider)
              .region(Region.EU_WEST_2)
              .build();
      case PROD ->
          SecretsManagerClient.builder()
              .credentialsProvider(credentialsProvider)
              .region(Region.EU_WEST_2)
              .build();
    };
  }

  @Bean
  public AccessManager accessManager(@Autowired UsersDao usersDao) {
    return new AccessManager(usersDao);
  }

  @Bean
  public RetryPolicy retryPolicy(
      @Value("${zk.backoffBase}") int backOffBase, @Value("${zk.backoffNRetries}") int nTrials) {
    return new ExponentialBackoffRetry(backOffBase, nTrials);
  }

  @Bean
  public FleetMetadata fleetMetadata(
      @Autowired CuratorFramework curatorFramework, @Autowired RetryPolicy retryPolicy) {
    return new FleetMetadataImpl(curatorFramework, retryPolicy);
  }

  @Bean
  public Clock clock(@Autowired ENV_TYPE envType) {
    return switch (envType) {
      case PROD -> new SystemClock();
      case INTEG_TEST -> new FakeClock(100);
      case TEST, ISO -> new SystemClock();
    };
  }

  @Bean
  public ShardsAndSeriesAssignerFactory shardsAndSeriesAssignerFactory() {
    return new ConsistentHashedAssignerFactory();
  }

  @Bean
  public CuratorFramework curatorFramework(
      @Value("${zk.connectionString}") String zkConnection,
      @Value("${zk.backoffBase}") int base,
      @Value("${zk.backoffNRetries}") int trial) {
    var client =
        CuratorFrameworkFactory.newClient(zkConnection, new ExponentialBackoffRetry(base, trial));
    client.start();
    return client;
  }

  @Bean
  public ZkResources zkResources(
      @Autowired CuratorFramework curatorFramework,
      @Autowired InterProcessLock clusterLock,
      @Autowired LeaderLatch leaderLatch)
      throws Exception {
    var resource = new ZkResourcesImpl(curatorFramework, clusterLock, leaderLatch);
    resource.init();
    return resource;
  }

  @Bean
  public Node node() {
    return new Node(
        "metrics-proxy-" + UUID.randomUUID().toString(), "do-not-use", NodeState.NODE_CREATED);
  }

  @Bean
  public ServiceRegistry serviceRegistry(
      @Autowired Clock clock,
      @Autowired FleetMetadata fleetMetadata,
      @Autowired Node self,
      @Autowired ZkResources zkResources) {
    return new ServiceRegistryImpl(clock, fleetMetadata, self, zkResources);
  }

  @Bean
  public AwsCredentialsProvider credentialsProvider(@Autowired ENV_TYPE ENV_TYPE) {
    return switch (ENV_TYPE) {
      case PROD ->
          software.amazon.awssdk.auth.credentials.InstanceProfileCredentialsProvider.create();
      case TEST, INTEG_TEST, ISO -> EnvironmentVariableCredentialsProvider.create();
    };
  }

  @Bean
  public InterProcessLock clusterLock(@Autowired CuratorFramework curatorFramework) {
    return new InterProcessMutex(curatorFramework, ZkPaths.clusterLock());
  }

  @Bean
  public LeaderLatch leaderLatch(@Autowired CuratorFramework curatorFramework) {
    return new LeaderLatch(curatorFramework, ZkPaths.metricsProxyLeader());
  }

  @Bean
  public AwsCredentialsProvider credentialsProviderV2(@Autowired ENV_TYPE ENV_TYPE) {
    return switch (ENV_TYPE) {
      case PROD -> InstanceProfileCredentialsProvider.create();
      case TEST, ISO -> EnvironmentVariableCredentialsProvider.create();
      case INTEG_TEST -> EnvironmentVariableCredentialsProvider.create();
    };
  }

  @Bean
  public DynamoDbClient dynamoDB(
      @Autowired ENV_TYPE envType, @Value("${aws.region}") String region) {

    return switch (envType) {
      case TEST, INTEG_TEST, ISO ->
          DynamoDbClient.builder()
              .endpointOverride(URI.create("http://localhost:4566"))
              .region(Region.of("eu-west-2")) // match your local endpoint region
              .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
              .build();

      case PROD ->
          DynamoDbClient.builder()
              .region(Region.of(region))
              .credentialsProvider(InstanceProfileCredentialsProvider.create())
              .build();
    };
  }

  @Bean
  public DynamoDbEnhancedClient dynamoDbEnhancedClient(@Autowired DynamoDbClient client) {
    return DynamoDbEnhancedClient.builder().dynamoDbClient(client).build();
  }

  @Bean
  public S3Client amazonS3v2(
      @Autowired ENV_TYPE ENV_TYPE,
      @Value(BeanIds.VALUE_REGION) String region,
      @Autowired AwsCredentialsProvider credentialsProviderV2) {

    return switch (ENV_TYPE) {
      case PROD ->
          S3Client.builder()
              .region(Region.of(region))
              .credentialsProvider(credentialsProviderV2)
              .build();

      case TEST, INTEG_TEST, ISO ->
          S3Client.builder()
              .endpointOverride(URI.create("http://localhost:4566"))
              .region(Region.of(region))
              .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
              .credentialsProvider(credentialsProviderV2)
              .build();
    };
  }

  @Bean
  public OkHttpClient httpClient() {
    return new OkHttpClient();
  }

  @Bean
  public Gson gson() {
    return new Gson();
  }

  @Bean
  public S3ByteRangeCache byteRangeCache(
      @Autowired S3Client amazonS3,
      @Value("${cacheExpiryMillis}") int cacheExpiryMillis,
      @Value("${maxCachedPages}") Integer maxCachedPages,
      @Value("${pageSize}") Long pageSize) {
    return new S3ByteRangeCache(
        amazonS3, maxCachedPages, pageSize, Duration.of(cacheExpiryMillis, ChronoUnit.MILLIS));
  }

  @Bean
  public MetricsDispatcher metricsDispatcher(
      @Autowired ZkRegistry listenerBasedRegistry,
      @Autowired OkHttpClient httpClient,
      @Autowired AuthorizationChecker authorizationChecker) {
    return new MetricsDispatcher(listenerBasedRegistry, httpClient, authorizationChecker);
  }

  @Bean
  public S3Enhanced s3Enhanced(
      @Autowired S3Client amazonS3, @Autowired S3ByteRangeCache s3ByteRangeCache) {
    return new S3Enhanced(s3ByteRangeCache, amazonS3);
  }

  @Bean
  public MetadataCache metadataCache(
      @Value("${dataBucket}") String bucket,
      @Autowired S3Enhanced s3Enhanced,
      @Autowired S3ByteRangeCache byteRangeCache,
      @Autowired S3Client amazonS3) {
    return new MetadataCache(bucket, s3Enhanced, byteRangeCache, amazonS3);
  }

  @Bean
  public AuthorizationTokenDao authorizationTokenDao(@Autowired DaoFactory daoFactory) {
    return daoFactory.authorizationTokenDao();
  }

  @Bean
  public AuthTokenCache authTokenCache(
      @Value("${lifeTimeMinutes}") int tokenLifeTimeMinutes,
      @Autowired AuthorizationTokenDao authorizationTokenDao) {
    var lifeTime = Duration.of(tokenLifeTimeMinutes, ChronoUnit.MINUTES);
    return new AuthTokenCache(lifeTime, authorizationTokenDao);
  }

  @Bean
  public AuthorizationChecker authorizationChecker(@Autowired AuthTokenCache authTokenCache) {
    return new AuthorizationChecker(authTokenCache);
  }

  @Bean
  public ZkRegistry zkListenerBasedRegistry(
      @Autowired CuratorFramework curatorFramework,
      @Autowired ServiceRegistry serviceRegistry,
      @Autowired ShardsAndSeriesAssignerFactory assignerFactory)
      throws Exception {
    return new ZkRegistry(curatorFramework, serviceRegistry, assignerFactory);
  }

  @Bean
  public ScanQueryProcessor scanQueryProcessor(
      @Autowired OkHttpClient okHttpClient,
      @Autowired Gson gson,
      @Autowired MetadataCache metadataCache,
      @Value("${dataBucket}") String dataBucket,
      @Autowired S3ByteRangeCache rangeCache,
      @Autowired AuthorizationChecker authorizationChecker,
      @Autowired ZkRegistry zkRegistry,
      @Autowired AccessManager accessManager,
      @Autowired TokenManager tokenManager,
      @Autowired TeamsDao teamsDao,
      @Autowired S3Client amazonS3) {
    return new ScanQueryProcessor(
        okHttpClient,
        gson,
        metadataCache,
        dataBucket,
        rangeCache,
        authorizationChecker,
        zkRegistry,
        amazonS3,
        tokenManager,
        accessManager,
        teamsDao);
  }

  @Bean
  public ClusterManager clusterManager(
      @Autowired AuthorizationChecker authorizationChecker,
      @Autowired ServiceRegistry serviceRegistry,
      @Autowired OkHttpClient okHttpClient,
      @Value("${orgId}") String orgId,
      @Value(BeanIds.VALUE_CLUSTER_ID) String clusterId) {
    return new ClusterManager(
        authorizationChecker, serviceRegistry, okHttpClient, orgId, new Gson(), clusterId);
  }

  @Bean
  public UserManager userManager(
      @Autowired UsersDao usersDao,
      @Autowired OrgDao orgDao,
      @Autowired TokenManager tokenManager,
      @Autowired TeamsManager teamsManager) {
    return new UserManager(usersDao, orgDao, tokenManager, teamsManager);
  }

  @Bean
  public TokenManager tokenManager(
      @Autowired Algorithm algorithm, @Autowired AccessManager accessManager) {
    return new TokenManager(algorithm, accessManager);
  }

  @Bean
  public DaoFactory daoFactory() {
    return ServiceLoader.load(DaoFactory.class).findFirst().get();
  }

  @Bean
  public OrgDao orgDao(@Autowired DaoFactory daoFactory) {
    return daoFactory.orgDao();
  }

  @Bean
  public UsersDao usersDao(@Autowired DaoFactory daoFactory) {
    return daoFactory.usersDao();
  }

  @Bean
  public SecretsManager secretsManager(@Autowired SecretsManagerClient secretsManagerClient) {
    return new SecretsManagerImpl(secretsManagerClient);
  }

  @Bean
  public TeamsDao teamsDao(@Autowired DaoFactory daoFactory) {
    return daoFactory.teamsDao();
  }

  @Bean
  public TeamMemberDao teamMemberDao(@Autowired DaoFactory daoFactory) {
    return daoFactory.teamMemberDao();
  }

  @Bean
  public RelationGraphDao relationGraphDao(@Autowired DaoFactory daoFactory) {
    return daoFactory.relationGraphDao();
  }

  @Bean
  public OrgManager orgManager(
      @Autowired TokenManager tokenManager,
      @Autowired AccessManager accessManager,
      @Autowired UsersDao usersDao,
      @Autowired RelationGraphDao relationGraphDao,
      @Autowired OrgDao orgDao) {
    return new OrgManager(tokenManager, accessManager, usersDao, orgDao, relationGraphDao);
  }

  @Bean
  public TeamsManager teamsManager(
      @Autowired TeamMemberDao teamMemberDao,
      @Autowired TeamsDao teamsDao,
      @Autowired UsersDao usersDao,
      @Autowired TokenManager tokenManager,
      @Autowired AccessManager accessManager,
      @Autowired RelationGraphDao relationGraphDao) {
    return new TeamsManager(
        accessManager, tokenManager, usersDao, teamsDao, teamMemberDao, relationGraphDao);
  }

  @Bean
  public AuthorizationTokenService authorizationTokenService(
      @Autowired AuthorizationTokenDao authorizationTokenDao,
      @Autowired TeamsDao teamsDao,
      @Autowired AccessManager accessManager,
      @Autowired TokenManager tokenManager,
      @Autowired OrgDao orgDao) {
    return new AuthorizationTokenService(
        authorizationTokenDao, teamsDao, orgDao, accessManager, tokenManager);
  }
}
