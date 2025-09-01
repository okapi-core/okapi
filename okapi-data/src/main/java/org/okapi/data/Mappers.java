package org.okapi.data;

import org.okapi.rest.auth.GetAuthorizationTokenResponse;
import org.okapi.rest.dashboards.GetDashboardResponse;
import org.okapi.rest.team.GetTeamResponse;
import org.okapi.rest.users.GetOrgUserView;
import org.okapi.rest.users.GetUserMetadataResponse;
import org.okapi.auth.RoleTemplates;
import org.okapi.data.dto.*;

import java.util.List;
import java.util.Objects;

public class Mappers {
  public static GetTeamResponse.GetTeamResponseBuilder mapDtoToBuilder(TeamDto dto) {
    return GetTeamResponse.builder()
        .orgId(dto.getOrgId())
        .teamId(dto.getTeamId())
        .teamName(dto.getTeamName())
        .description(dto.getDescription())
        .createdAt(dto.getCreatedAt());
  }

  public static GetTeamResponse mapTeamDtoToResponse(TeamDto dto) {
    return mapDtoToBuilder(dto).build();
  }

  public static GetOrgUserView getOrgUserView(String userId, OrgDto orgDto) {
    return GetOrgUserView.builder()
        .orgId(orgDto.getOrgId())
        .isCreator(userId.equals(orgDto.getOrgCreator()))
        .orgName(orgDto.getOrgName())
        .build();
  }

  public static GetUserMetadataResponse buildUserMetadataResponse(
          List<GetOrgUserView> userOrgs, UserDto userDto) {
    return GetUserMetadataResponse.builder()
        .orgs(userOrgs)
        .firstName(userDto.getFirstName())
        .lastName(userDto.getLastName())
        .build();
  }

  public static GetAuthorizationTokenResponse mapAuthorizationTokenDto(AuthorizationTokenDto dto) {
    var roles =
        dto.getAuthorizationRoles().stream()
            .map(
                role -> {
                  var parsed = RoleTemplates.parseAsTokenRole(role);
                  return parsed
                      .map(
                          parsedTokenRole ->
                              switch (parsedTokenRole.apiTokenPermission()) {
                                case READER -> "reader";
                                case WRITER -> "writer";
                              })
                      .orElse(null);
                })
            .filter(Objects::nonNull)
            .toList();
    return GetAuthorizationTokenResponse.builder()
        .roles(roles)
        .createdAt(dto.getCreated())
        .authorizationToken(dto.getAuthorizationToken())
        .build();
  }

  public static GetDashboardResponse mapDashboardDtoToResponse(
      String definition, DashboardDto dashboardDto) {
    return GetDashboardResponse.builder()
        .id(dashboardDto.getDashboardId())
        .definition(definition)
        .title(dashboardDto.getDashboardTitle())
        .note(dashboardDto.getDashboardNote())
        .created(dashboardDto.getCreated())
        .updated(dashboardDto.getUpdatedTime())
        .build();
  }
}
