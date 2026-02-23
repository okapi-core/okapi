package org.okapi.web.dtos.auth;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Builder
public class GetUserProfileResponse {
  @NotNull
  String id;
  String firstName;
  String lastName;
  @NotNull
  String email;
}
