package org.okapi.data.dto;

import java.util.Date;
import lombok.*;

@Builder(toBuilder = true)
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Setter
public class UserRoleRelationDto {

  public enum STATUS {
    ACTIVE,
    INACTIVE
  }

  String userId;
  String role;
  Date createdAt;
  STATUS status;
}
