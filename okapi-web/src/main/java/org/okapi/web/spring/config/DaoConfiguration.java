package org.okapi.web.spring.config;

import org.okapi.data.dao.*;
import org.okapi.data.ddb.*;
import org.okapi.data.ddb.dao.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
public class DaoConfiguration {

  @Bean
  public DynamoDbEnhancedClient enhancedClient(@Autowired DynamoDbClient dynamoDbClient) {
    return DynamoDbEnhancedClient.builder().dynamoDbClient(dynamoDbClient).build();
  }

  @Bean
  public ResultUploader resultUploader(@Autowired S3Client s3Client, @Autowired S3Cfg s3Cfg) {
    return new S3ResultUploader(s3Client, s3Cfg.getBucket(), s3Cfg.getResultsPrefix());
  }

  @Bean
  public UsersDao usersDao(@Autowired DynamoDbEnhancedClient enhancedClient) {
    return new UsersDaoImpl(enhancedClient);
  }

  @Bean
  public OrgDao orgDao(@Autowired DynamoDbEnhancedClient enhancedClient) {
    return new OrgDaoDdbImpl(enhancedClient);
  }

  @Bean
  public RelationGraphDao relationGraphDao(@Autowired DynamoDbEnhancedClient enhancedClient) {
    return new RelationGraphDaoImpl(enhancedClient);
  }

  @Bean
  public DashboardDao dashboardDao(@Autowired DynamoDbEnhancedClient enhancedClient) {
    return new DashboardDaoImpl(enhancedClient);
  }

  @Bean
  public FederatedSourceRepo federatedSourceRepo(@Autowired DynamoDbEnhancedClient enhancedClient) {
    return new FederatedSourceRepoImpl(enhancedClient, new CommonQueryPatterns<>());
  }

  @Bean
  public DashboardRowDao dashboardRowDao(@Autowired DynamoDbEnhancedClient enhancedClient) {
    return new DashboardRowDaoDdbImpl(enhancedClient);
  }

  @Bean
  public DashboardPanelDao dashboardPanelDao(@Autowired DynamoDbEnhancedClient enhancedClient) {
    return new DashboardPanelDaoDdbImpl(enhancedClient);
  }

  @Bean
  public DashboardDao dashboardDaoDdb(@Autowired DynamoDbEnhancedClient enhancedClient) {
    return new DashboardDaoImpl(enhancedClient);
  }

  @Bean
  public UserEntityRelationsDao userEntityRelationsDao(
      @Autowired DynamoDbEnhancedClient enhancedClient) {
    return new UserEntityRelationsDaoImpl(enhancedClient);
  }

  @Bean
  public PendingJobsDao pendingJobsDao(
      @Autowired DynamoDbEnhancedClient enhancedClient, @Autowired ResultUploader resultUploader) {
    return new PendingJobsDaoDdbImpl(enhancedClient, resultUploader);
  }

  @Bean
  public TokenMetaDao tokenMetaDao(@Autowired DynamoDbEnhancedClient enhancedClient) {
    return new TokenMetaDaoDdbImpl(enhancedClient);
  }

  @Bean
  public DashboardVarDao dashboardVarDao(@Autowired DynamoDbEnhancedClient enhancedClient) {
    return new DashboardVarDaoImpl(enhancedClient);
  }

  @Bean
  public DashboardVersionDao dashboardVersionDao(@Autowired DynamoDbEnhancedClient enhancedClient) {
    return new DashboardVersionDaoImpl(enhancedClient);
  }
}
