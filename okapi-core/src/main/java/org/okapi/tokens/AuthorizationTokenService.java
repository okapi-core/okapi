package org.okapi.tokens;

import com.google.common.collect.Lists;
import org.okapi.auth.AccessManager;
import org.okapi.auth.JwtClaims;
import org.okapi.auth.RoleTemplates;
import org.okapi.auth.TokenManager;
import org.okapi.data.Mappers;
import org.okapi.data.dao.AuthorizationTokenDao;
import org.okapi.data.dao.OrgDao;
import org.okapi.data.dao.TeamsDao;
import org.okapi.data.dto.AuthorizationTokenDto;
import org.okapi.exceptions.UnAuthorizedException;
import org.okapi.rest.auth.GetAuthorizationTokenResponse;
import org.okapi.rest.auth.ListAuthorizationTokensResponse;
import org.okapi.rest.tokens.CreateApiTokenRequest;
import org.okapi.validation.OkapiChecks;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class AuthorizationTokenService {
  public static final Duration API_TOKEN_EXPIRY = Duration.of(365, ChronoUnit.DAYS);
  public static final Duration ADMIN_TOKEN_EXPIRY = Duration.of(6, ChronoUnit.HOURS);

  AuthorizationTokenDao authorizationTokenDao;
  TeamsDao teamsDao;
  OrgDao orgDao;
  AccessManager accessManager;
  TokenManager tokenManager;

  public GetAuthorizationTokenResponse get(String token) throws UnAuthorizedException {
    var authorizationToken = authorizationTokenDao.findToken(token);
    if (authorizationToken.isEmpty()) {
      throw new UnAuthorizedException("Token not found");
    }
    return Mappers.mapAuthorizationTokenDto(authorizationToken.get());
  }

  public GetAuthorizationTokenResponse createApiToken(
      String tempToken, String teamId, CreateApiTokenRequest apiTokenRequest)
      throws UnAuthorizedException {
    // user should be team admin, the associated org is the one associated with the team
    var userId = tokenManager.checkClaimOrThrow(tempToken, JwtClaims.CLAIM_USER_ID);
    var team = teamsDao.get(teamId);
    if (team.isEmpty()) {
      throw new UnAuthorizedException("Team not found");
    }
    var orgId = team.get().getOrgId();
    var randomToken = tokenManager.generateAuthorizationToken();
    var roles = new ArrayList<String>();
    if (apiTokenRequest.isCanRead()) {
      roles.add(RoleTemplates.getTeamReaderRole(orgId, teamId));
    }
    if (apiTokenRequest.isCanWrite()) {
      roles.add(RoleTemplates.getTeamWriterRole(orgId, teamId));
    }

    var authTokenDto =
        AuthorizationTokenDto.builder()
            .authorizationToken(randomToken)
            .tokenStatus(AuthorizationTokenDto.AuthorizationTokenStatus.ACTIVE)
            .orgId(orgId)
            .teamId(teamId)
            .issuer(userId)
            .authorizationRoles(roles)
            .expiryTime(System.currentTimeMillis() + API_TOKEN_EXPIRY.toMillis())
            .build();
    authorizationTokenDao.putToken(authTokenDto);
    return Mappers.mapAuthorizationTokenDto(authTokenDto);
  }

  public GetAuthorizationTokenResponse createAdminToken(String tempToken, String orgId)
      throws UnAuthorizedException {
    var userId = tokenManager.checkClaimOrThrow(tempToken, JwtClaims.CLAIM_USER_ID);
    var org = orgDao.findById(orgId);
    OkapiChecks.checkArgument(org.isPresent(), UnAuthorizedException::new);
    accessManager.checkUserIsClusterAdmin(userId, org.get().getOrgId());
    var randomToken = tokenManager.generateAuthorizationToken();
    var authTokenDto =
        AuthorizationTokenDto.builder()
            .authorizationToken(randomToken)
            .tokenStatus(AuthorizationTokenDto.AuthorizationTokenStatus.ACTIVE)
            .orgId(orgId)
            .issuer(userId)
            .expiryTime(System.currentTimeMillis() + ADMIN_TOKEN_EXPIRY.toMillis())
            .authorizationRoles(Arrays.asList(RoleTemplates.getClusterAdminRole(orgId)))
            .build();
    authorizationTokenDao.putToken(authTokenDto);
    return Mappers.mapAuthorizationTokenDto(authTokenDto);
  }

  public ListAuthorizationTokensResponse list(String token, String teamId)
      throws UnAuthorizedException {
    var userId = tokenManager.checkClaimOrThrow(token, JwtClaims.CLAIM_USER_ID);
    var team = teamsDao.get(teamId);
    if (team.isEmpty()) {
      throw new UnAuthorizedException("Team not found");
    }
    accessManager.checkUserIsTeamAdmin(userId, team.get().getOrgId(), team.get().getTeamId());
    var tokens =
        Lists.newArrayList(authorizationTokenDao.listTokenByTeam(teamId)).stream()
            .filter(
                t -> t.getTokenStatus() == AuthorizationTokenDto.AuthorizationTokenStatus.ACTIVE)
            .toList();
    return ListAuthorizationTokensResponse.builder()
        .tokens(tokens.stream().map(Mappers::mapAuthorizationTokenDto).toList())
        .build();
  }

  public void delete(String tempToken, String authorizationToken) throws UnAuthorizedException {
    var userId = tokenManager.checkClaimOrThrow(tempToken, JwtClaims.CLAIM_USER_ID);
    var authToken = authorizationTokenDao.findToken(authorizationToken);
    var teamId =
        authToken.orElseThrow(() -> new UnAuthorizedException("Token not found")).getTeamId();
    var team = teamsDao.get(teamId);
    if (team.isEmpty()) {
      throw new UnAuthorizedException("Team not found");
    }
    accessManager.checkUserIsTeamAdmin(userId, team.get().getOrgId(), team.get().getTeamId());
    authorizationTokenDao.deleteToken(authorizationToken);
  }

}
