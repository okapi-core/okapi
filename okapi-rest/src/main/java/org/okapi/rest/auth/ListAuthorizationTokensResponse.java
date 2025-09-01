package org.okapi.rest.auth;
    
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
        private List<GetAuthorizationTokenResponse> tokens;
    }