package org.okapi.web.dtos.token;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@Getter
@NoArgsConstructor
@Builder
public class GetTokenResponse {
  String tokenId;
  String tokenStatus;
  Long createdAt;
}
