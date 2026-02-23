package org.okapi.web.dtos.auth;

import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Builder(toBuilder = true)
@AllArgsConstructor
@Getter
public class GetAuthorizationTokenResponse {
  String authorizationToken;
  Instant createdAt;
  List<String> roles;
}
