package org.okapi.data.dto;

import com.google.common.base.Preconditions;
import java.time.Instant;
import java.util.List;
import lombok.*;

@NoArgsConstructor
@Builder
@Getter
@Setter
public class AuthorizationTokenDto {
  public enum AuthorizationTokenStatus {
    ACTIVE,
    INACTIVE
  };

  String authorizationToken;
  AuthorizationTokenStatus tokenStatus;
  String orgId;
  String teamId;
  String issuer;
  Instant created;
  Long expiryTime;
  List<String> authorizationRoles;

  public void setAuthorizationRoles(List<String> authorizationRoles) {
    this.authorizationRoles = authorizationRoles;
  }

  public AuthorizationTokenDto setAuthorizationToken(String authorizationToken) {
    Preconditions.checkNotNull(authorizationToken, "authorizationToken must not be null");
    this.authorizationToken = authorizationToken;
    return this;
  }

  public AuthorizationTokenDto setTokenStatus(AuthorizationTokenStatus tokenStatus) {
    Preconditions.checkNotNull(tokenStatus, "tokenStatus must not be null");
    this.tokenStatus = tokenStatus;
    return this;
  }

  public AuthorizationTokenDto setOrgId(String orgId) {
    Preconditions.checkNotNull(orgId, "orgId must not be null");
    this.orgId = orgId;
    return this;
  }

  public AuthorizationTokenDto setTeamId(String teamId) {
    this.teamId = teamId;
    return this;
  }

  public AuthorizationTokenDto setCreated(Instant created) {
    this.created = created;
    return this;
  }

  public AuthorizationTokenDto setIssuer(String issuer) {
    Preconditions.checkNotNull(issuer, "issuer must not be null");
    this.issuer = issuer;
    return this;
  }

  public AuthorizationTokenDto setExpiryTime(Long expiryTime) {
    Preconditions.checkNotNull(expiryTime);
    this.expiryTime = expiryTime;
    return this;
  }

  public AuthorizationTokenDto setAuthTokenType(List<String> authorizationRoles) {
    Preconditions.checkNotNull(authorizationRoles);
    this.authorizationRoles = authorizationRoles;
    return this;
  }

  public AuthorizationTokenDto(
      String authorizationToken,
      AuthorizationTokenStatus tokenStatus,
      String orgId,
      String teamId,
      String issuer,
      Instant created,
      Long expiryTime,
      List<String> authorizationRoles) {
    setAuthTokenType(authorizationRoles);
    this.setAuthorizationToken(authorizationToken);
    this.setTokenStatus(tokenStatus);
    this.setOrgId(orgId);
    this.setTeamId(teamId);
    this.setCreated(created);
    this.setIssuer(issuer);
    setExpiryTime(expiryTime);
  }
}
