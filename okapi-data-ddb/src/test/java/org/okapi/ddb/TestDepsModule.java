package org.okapi.ddb;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import org.okapi.data.dao.ResultUploader;
import org.okapi.testutils.OkapiTestUtils;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

public class TestDepsModule extends AbstractModule {

  @Provides
  @Singleton
  public DynamoDbEnhancedClient enhancedClient(DynamoDbClient client) {
    return DynamoDbEnhancedClient.builder().dynamoDbClient(client).build();
  }

  @Provides
  @Singleton
  public DynamoDbClient dynamoDbClient() {
    return OkapiTestUtils.getLocalStackDynamoDbClient();
  }

  @Provides
  @Singleton
  public ResultUploader fakeResultUploader() {
    return new FakeResultUploader();
  }
}
