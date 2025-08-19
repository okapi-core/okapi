package org.okapi.data.dto;

import com.google.common.base.Preconditions;
import lombok.*;

@Builder(toBuilder = true)
@Data
public class UserDto {
  public enum UserStatus {
    ACTIVE,
    NOT_ACTIVE
  }

  String userId;
  String email;
  UserStatus status;
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

  public void setStatus(UserStatus status) {
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
}
