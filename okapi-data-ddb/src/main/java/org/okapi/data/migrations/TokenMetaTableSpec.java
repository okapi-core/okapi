/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.data.migrations;

import static org.okapi.data.TableSpecifications.makeRequest;
import static org.okapi.data.dto.TableAttributes.ORG_ID;
import static org.okapi.data.dto.TableAttributes.TOKEN_ID;
import static org.okapi.data.dto.TableAttributes.TOKEN_STATUS;
import static org.okapi.data.dto.TablesAndIndexes.TOKEN_META_BY_ORG_STATUS_GSI;
import static org.okapi.data.dto.TablesAndIndexes.TOKEN_META_TABLE;

import java.util.Arrays;
import org.okapi.data.TableSpec;
import org.okapi.data.dto.TokenMetaDdb;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;

public class TokenMetaTableSpec implements TableSpec<TokenMetaDdb> {

  @Override
  public CreateTableRequest getSpec() {
    return makeRequest(
        TOKEN_META_TABLE,
        ORG_ID,
        TOKEN_ID,
        Arrays.asList(TOKEN_META_BY_ORG_STATUS_GSI),
        Arrays.asList(ORG_ID),
        Arrays.asList(TOKEN_STATUS));
  }

  @Override
  public String getName() {
    return TOKEN_META_TABLE;
  }
}
