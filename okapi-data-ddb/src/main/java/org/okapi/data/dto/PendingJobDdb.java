/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.data.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.okapi.data.ddb.attributes.serialization.DataSourceQueryConverter;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;

@DynamoDbBean
@Setter
@AllArgsConstructor
@Builder(toBuilder = true)
@NoArgsConstructor
public class PendingJobDdb {

  String orgId;
  String jobId;
  String resultS3;
  String errorS3;
  JOB_STATUS jobStatus;
  String sourceId;
  DataSourceQuery query;
  int attemptCount;
  Long createdAt;
  Long assignedAt;
  String orgSourceStatusKey;

  @DynamoDbPartitionKey
  @DynamoDbAttribute(TableAttributes.ORG_ID)
  public String getOrgId() {
    return orgId;
  }

  @DynamoDbSortKey
  @DynamoDbAttribute(TableAttributes.JOB_ID)
  public String getJobId() {
    return jobId;
  }

  @DynamoDbAttribute(TableAttributes.RESULT_LOCATION)
  public String getResultS3() {
    return resultS3;
  }

  @DynamoDbAttribute(TableAttributes.JOB_STATUS)
  public JOB_STATUS getJobStatus() {
    return jobStatus;
  }

  @DynamoDbAttribute(TableAttributes.SOURCE_ID)
  public String getSourceId() {
    return sourceId;
  }

  @DynamoDbAttribute(TableAttributes.ATTEMPT_COUNT)
  public int getAttemptCount() {
    return attemptCount;
  }

  @DynamoDbAttribute(TableAttributes.CREATED_TIME)
  public Long getCreatedAt() {
    return createdAt;
  }

  @DynamoDbAttribute(TableAttributes.ASSIGNED_TIME)
  public Long getAssignedAt() {
    return assignedAt;
  }

  @DynamoDbAttribute(TableAttributes.ORG_SOURCE_STATUS_KEY)
  @DynamoDbSecondaryPartitionKey(indexNames = TablesAndIndexes.PENDING_JOBS_BY_SOURCE_STATUS_GSI)
  public String getOrgSourceStatusKey() {
    return orgSourceStatusKey;
  }

  @DynamoDbAttribute(TableAttributes.DATA_SOURCE_QUERY)
  @DynamoDbConvertedBy(DataSourceQueryConverter.class)
  public DataSourceQuery getQuery() {
    return query;
  }

  @DynamoDbAttribute(TableAttributes.ERROR_LOCATION)
  public String getErrorS3() {
    return errorS3;
  }

  public static String buildOrgSourceStatusKey(String orgId, String sourceId, JOB_STATUS status) {
    return orgId + "#" + sourceId + "#" + status.name();
  }
}
