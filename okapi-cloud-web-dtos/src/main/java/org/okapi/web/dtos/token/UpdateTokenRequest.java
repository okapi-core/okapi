package org.okapi.web.dtos.token;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@Getter
@NoArgsConstructor
public class UpdateTokenRequest {
  String tokenId;
  String status;
}
