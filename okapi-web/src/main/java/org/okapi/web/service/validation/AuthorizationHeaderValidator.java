package org.okapi.web.service.validation;

import lombok.AllArgsConstructor;
import org.okapi.exceptions.UnAuthorizedException;
import org.okapi.web.auth.ApiTokenManager;
import org.okapi.web.auth.AuthorizedEntity;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class AuthorizationHeaderValidator {
  ApiTokenManager apiTokenManager;

  public AuthorizedEntity getAuthorizedEntity(String authorizationHeader) {
    var parts = authorizationHeader.split(" ");
    var headerType = parts.length > 0 ? parts[0] : "";
    var header = parts.length > 1 ? parts[1] : "";
    if (headerType.equals("Bearer")) {
      return apiTokenManager.authorize(header);
    }
    throw new UnAuthorizedException("Invalid Authorization header");
  }
}
