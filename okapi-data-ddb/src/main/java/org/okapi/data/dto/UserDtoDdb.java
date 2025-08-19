package org.okapi.data.dto;

import com.google.common.base.Preconditions;
import lombok.*;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;

@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@DynamoDbBean
public class UserDtoDdb {
  String userId;
  String email;
  UserDto.UserStatus status;
  String firstName;
  String lastName;
  String hashedPassword;

  public void setUserId(String userId) {
    Preconditions.checkNotNull(userId);
    this.userId = userId;
  }

  public void setEmail(String email) {
    Preconditions.checkNotNull(email);
    this.email = email;
  }

  public void setStatus(UserDto.UserStatus status) {
    Preconditions.checkNotNull(status);
    this.status = status;
  }

  public void setFirstName(String firstName) {
    Preconditions.checkNotNull(firstName);
    this.firstName = firstName;
  }

  public void setLastName(String lastName) {
    Preconditions.checkNotNull(lastName);
    this.lastName = lastName;
  }

  public void setHashedPassword(String hashedPassword) {
    Preconditions.checkNotNull(hashedPassword);
    this.hashedPassword = hashedPassword;
  }

  @DynamoDbAttribute(TableAttributes.USER_ID)
  @DynamoDbPartitionKey
  public String getUserId() {
    return userId;
  }

  @DynamoDbSecondaryPartitionKey(indexNames = TablesAndIndexes.USERS_BY_EMAIL_GSI)
  @DynamoDbAttribute(TableAttributes.EMAIL)
  public String getEmail() {
    return email;
  }

  @DynamoDbAttribute(TableAttributes.USER_STATUS)
  public UserDto.UserStatus getStatus() {
    return status;
  }

  @DynamoDbAttribute(TableAttributes.FIRST_NAME)
  public String getFirstName() {
    return firstName;
  }

  @DynamoDbAttribute(TableAttributes.LAST_NAME)
  public String getLastName() {
    return lastName;
  }

  @DynamoDbAttribute(TableAttributes.HASHED_PW)
  public String getHashedPassword() {
    return hashedPassword;
  }
}
