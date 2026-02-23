/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.data.migrations;

import static org.okapi.data.TableSpecifications.makeRequest;
import static org.okapi.data.TableSpecifications.nullRangeKeys;
import static org.okapi.data.dto.TableAttributes.JOB_ID;
import static org.okapi.data.dto.TableAttributes.ORG_ID;
import static org.okapi.data.dto.TableAttributes.ORG_SOURCE_STATUS_KEY;
import static org.okapi.data.dto.TablesAndIndexes.PENDING_JOBS_BY_SOURCE_STATUS_GSI;
import static org.okapi.data.dto.TablesAndIndexes.PENDING_JOBS_TABLE;

import java.util.Arrays;
import org.okapi.data.TableSpec;
import org.okapi.data.dto.PendingJobDdb;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;

public class PendingJobsTableSpec implements TableSpec<PendingJobDdb> {
  @Override
  public CreateTableRequest getSpec() {
    return makeRequest(
        PENDING_JOBS_TABLE,
        ORG_ID,
        JOB_ID,
        Arrays.asList(PENDING_JOBS_BY_SOURCE_STATUS_GSI),
        Arrays.asList(ORG_SOURCE_STATUS_KEY),
        nullRangeKeys(1));
  }

  @Override
  public String getName() {
    return PENDING_JOBS_TABLE;
  }
}
