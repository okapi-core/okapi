package org.okapi.web.dtos.token;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@AllArgsConstructor
@Builder
@Getter
public class ListTokensResponse {
    List<GetTokenResponse> tokens;
}
