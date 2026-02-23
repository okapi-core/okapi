package org.okapi.data.migrations;

import static org.okapi.data.TableSpecifications.makeRequest;
import static org.okapi.data.TableSpecifications.nullRangeKeys;
import static org.okapi.data.dto.TableAttributes.EMAIL;
import static org.okapi.data.dto.TableAttributes.USER_ID;
import static org.okapi.data.dto.TablesAndIndexes.USERS_BY_EMAIL_GSI;
import static org.okapi.data.dto.TablesAndIndexes.USERS_TABLE;

import java.util.Arrays;
import org.okapi.data.dto.UserDtoDdb;
import org.okapi.data.TableSpec;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;

public class UsersTableSpec implements TableSpec<UserDtoDdb> {
  @Override
  public CreateTableRequest getSpec() {
    return makeRequest(
        USERS_TABLE,
        USER_ID,
        null,
        Arrays.asList(USERS_BY_EMAIL_GSI),
        Arrays.asList(EMAIL),
        nullRangeKeys(1));
  }

  @Override
  public String getName() {
    return USERS_TABLE;
  }
}
