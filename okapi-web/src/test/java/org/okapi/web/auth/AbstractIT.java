package org.okapi.web.auth;

import java.util.UUID;
import org.okapi.data.CreateDynamoDBTables;
import org.okapi.data.CreateS3Bucket;
import org.okapi.exceptions.UnAuthorizedException;
import org.okapi.fixtures.Deduplicator;
import org.okapi.web.spring.config.S3Cfg;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.s3.S3Client;

@SpringBootTest
public class AbstractIT {
  @Autowired DynamoDbClient dynamoDbClient;
  @Autowired S3Client s3Client;

  @Autowired S3Cfg s3Cfg;

  String testInstance = UUID.randomUUID().toString();

  public void setup() throws UnAuthorizedException {
    CreateDynamoDBTables.createTables(dynamoDbClient);
    CreateS3Bucket.createS3Bucket(s3Client, s3Cfg.getBucket());
  }

  public String dedup(String val, Class<?> cls) {
    return Deduplicator.dedup(testInstance, val, cls);
  }
}
