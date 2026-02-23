package org.okapi.web.dtos.auth;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
public class ListAuthorizationTokensResponse {
  @NotNull
  private List<GetAuthorizationTokenResponse> tokens;
}
