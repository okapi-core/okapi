package org.okapi.auth;

import static org.okapi.data.dto.RelationGraphNode.ENTITY_TYPE.TEAM;
import static org.okapi.data.dto.RelationGraphNode.ENTITY_TYPE.USER;
import static org.okapi.data.dto.RelationGraphNode.RELATION_TYPE.TEAM_ADMIN;
import static org.okapi.data.dto.RelationGraphNode.RELATION_TYPE.TEAM_MEMBER;
import static org.okapi.data.dto.RelationGraphNode.makeEntityId;

import com.google.common.collect.Lists;
import com.okapi.rest.org.UpdateTeamRequest;
import com.okapi.rest.team.*;
import java.util.*;
import lombok.AllArgsConstructor;
import org.okapi.collections.OkapiOptionalUtils;
import org.okapi.data.Mappers;
import org.okapi.data.dao.RelationGraphDao;
import org.okapi.data.dao.TeamMemberDao;
import org.okapi.data.dao.TeamsDao;
import org.okapi.data.dao.UsersDao;
import org.okapi.data.dto.TeamDto;
import org.okapi.data.exceptions.TeamNotFoundException;
import org.okapi.exceptions.BadRequestException;
import org.okapi.exceptions.UnAuthorizedException;
import org.okapi.metrics.IdCreationFailedException;
import org.okapi.metrics.IdCreator;
import org.okapi.usermessages.UserFacingMessages;

@AllArgsConstructor
public class TeamsManager {

  AccessManager accessManager;
  TokenManager tokenManager;
  UsersDao usersDao;
  TeamsDao teamsDao;
  TeamMemberDao teamMemberDao;
  RelationGraphDao relationGraphDao;

  public GetTeamResponse createTeam(String tempBearerToken, CreateTeamRequest request)
      throws UnAuthorizedException, IdCreationFailedException {
    var userId = tokenManager.getUserId(tempBearerToken);
    var orgId = tokenManager.getOrgId(tempBearerToken);
    accessManager.checkUserHasIsOrgAdmin(userId, orgId);
    var temId =
        IdCreator.generateTeamId(
            (generated) -> {
              return teamsDao.get(generated).isPresent();
            });
    var teamDto =
        TeamDto.builder()
            .teamId(temId)
            .teamName(request.getName())
            .description(request.getDescription())
            .orgId(orgId)
            .build();
    teamsDao.create(teamDto);
    usersDao.grantRole(userId, RoleTemplates.getTeamAdminRole(orgId, teamDto.getTeamId()));
    usersDao.grantRole(userId, RoleTemplates.getTeamWriterRole(orgId, teamDto.getTeamId()));
    usersDao.grantRole(userId, RoleTemplates.getTeamReaderRole(orgId, teamDto.getTeamId()));
    var userEntity = makeEntityId(USER, userId);
    var teamEntity = makeEntityId(TEAM, temId);
    relationGraphDao.addAll(userEntity, teamEntity, Arrays.asList(TEAM_MEMBER, TEAM_ADMIN));
    return Mappers.mapTeamDtoToResponse(teamDto);
  }

  public void updateTeam(String tempBearerToken, String teamId, UpdateTeamRequest request)
      throws UnAuthorizedException, BadRequestException {
    var userId = tokenManager.getUserId(tempBearerToken);
    var team = teamsDao.get(teamId);
    if (team.isEmpty()) {
      throw new BadRequestException(UserFacingMessages.TEAM_NOT_FOUND);
    }

    accessManager.checkUserIsTeamAdmin(userId, team.get().getOrgId(), teamId);
    TeamDto teamDto = null;
    try {
      teamDto = teamsDao.update(teamId, request.getName(), request.getDescription());
    } catch (TeamNotFoundException e) {
      throw new BadRequestException(UserFacingMessages.TEAM_NOT_FOUND);
    }
    if (teamDto == null) {
      throw new BadRequestException(UserFacingMessages.TEAM_NOT_FOUND);
    }
  }

  public ListTeamsResponse listTeams(String tempBearerToken)
      throws UnAuthorizedException, BadRequestException {
    var userId = tokenManager.getUserId(tempBearerToken);
    // for this user list all teams that they are a part of
    var roles = Lists.newArrayList(usersDao.listRolesByUserId(userId));
    var teamProtoTypes = new HashMap<String, GetTeamResponse.GetTeamResponseBuilder>();
    var adminTeams = new HashSet<String>();
    var readerTeams = new HashSet<String>();
    var writerTeams = new HashSet<String>();
    for (var role : roles) {
      var isAdmin = RoleTemplates.isTeamAdmin(role.getRole());
      var isWriter = RoleTemplates.isTeamWriter(role.getRole());
      var isReader = RoleTemplates.isTeamReader(role.getRole());
      var value = OkapiOptionalUtils.findPresent(Arrays.asList(isAdmin, isWriter, isReader));
      if (value.isPresent()) {
        var teamId = value.get();
        var dto = teamsDao.get(teamId);
        if (dto.isEmpty()) throw new BadRequestException();
        var builder = Mappers.mapDtoToBuilder(dto.get());
        teamProtoTypes.put(teamId, builder);
        if (isAdmin.isPresent()) adminTeams.add(teamId);
        if (isWriter.isPresent()) writerTeams.add(teamId);
        if (isReader.isPresent()) readerTeams.add(teamId);
      }
    }
    var teamsList =
        teamProtoTypes.keySet().stream()
            .map(
                teamId -> {
                  var prototype = teamProtoTypes.get(teamId);
                  prototype.admin(adminTeams.contains(teamId));
                  prototype.writer(writerTeams.contains(teamId));
                  prototype.reader(readerTeams.contains(teamId));
                  return prototype.build();
                })
            .toList();
    return new ListTeamsResponse(teamsList);
  }

  public GetTeamResponse getTeam(String tempBearerToken, String teamId)
      throws UnAuthorizedException, BadRequestException {
    var userId = tokenManager.getUserId(tempBearerToken);
    var team = teamsDao.get(teamId);
    if (team.isEmpty()) {
      throw new BadRequestException(UserFacingMessages.TEAM_NOT_FOUND);
    }
    var orgId = team.get().getOrgId();
    accessManager.checkUserIsTeamAdmin(userId, orgId, teamId);
    return Mappers.mapTeamDtoToResponse(team.get());
  }

  public void addTeamMember(String tempBearerToken, String teamId, CreateTeamMemberRequest request)
      throws UnAuthorizedException, BadRequestException {
    var userId = tokenManager.getUserId(tempBearerToken);
    var team = teamsDao.get(teamId);
    if (team.isEmpty()) {
      throw new BadRequestException(UserFacingMessages.TEAM_NOT_FOUND);
    }
    var orgId = team.get().getOrgId();

    accessManager.checkUserIsTeamAdmin(userId, orgId, teamId);
    var memberUser = usersDao.getWithEmail(request.getEmail());
    if (memberUser.isEmpty()) {
      throw new BadRequestException(UserFacingMessages.USER_NOT_FOUND);
    }
    var userDto = memberUser.get();
    teamMemberDao.addMember(teamId, userDto.getUserId());
    if (request.isWriter()) {
      usersDao.grantRole(userDto.getUserId(), RoleTemplates.getTeamWriterRole(orgId, teamId));
    }
    if (request.isAdmin()) {
      usersDao.grantRole(userDto.getUserId(), RoleTemplates.getTeamAdminRole(orgId, teamId));
    }
    if (request.isReader()) {
      usersDao.grantRole(userDto.getUserId(), RoleTemplates.getTeamReaderRole(orgId, teamId));
    }
  }

  public void deleteTeamMember(String tempBearerToken, String teamId, String email)
      throws UnAuthorizedException, BadRequestException {
    var userId = tokenManager.getUserId(tempBearerToken);
    var team = teamsDao.get(teamId);
    if (team.isEmpty()) {
      throw new BadRequestException(UserFacingMessages.TEAM_NOT_FOUND);
    }
    var orgId = team.get().getOrgId();
    accessManager.checkUserIsTeamAdmin(userId, orgId, teamId);
    var memberUser = usersDao.getWithEmail(email);
    if (memberUser.isEmpty()) {
      throw new BadRequestException(UserFacingMessages.USER_NOT_FOUND);
    }
    var userDto = memberUser.get();
    var memberUserId = userDto.getUserId();
    teamMemberDao.removeMember(teamId, memberUserId);
    var teamEntity = makeEntityId(TEAM, teamId);
    var userEntity = makeEntityId(USER, userId);
    relationGraphDao.removeAll(userEntity, teamEntity);
    var allRoles =
        Arrays.asList(
            RoleTemplates.getTeamReaderRole(orgId, teamId),
            RoleTemplates.getTeamWriterRole(orgId, teamId),
            RoleTemplates.getTeamAdminRole(orgId, teamId));
    for (var role : allRoles) {
      usersDao.revokeRole(memberUserId, role);
    }
  }

  public ListTeamMembersResponse listTeamMembers(String tempBearerToken, String teamId)
      throws UnAuthorizedException, BadRequestException {
    var userId = tokenManager.getUserId(tempBearerToken);
    var team = teamsDao.get(teamId);
    if (team.isEmpty()) {
      throw new BadRequestException(UserFacingMessages.TEAM_NOT_FOUND);
    }
    var orgId = team.get().getOrgId();
    accessManager.checkUserIsTeamAdmin(userId, orgId, teamId);
    var teamAdminRole = RoleTemplates.getTeamAdminRole(orgId, teamId);
    var teamReaderRole = RoleTemplates.getTeamReaderRole(orgId, teamId);
    var teamWriterRole = RoleTemplates.getTeamWriterRole(orgId, teamId);
    var adminUserEmails = Lists.newArrayList(usersDao.listUsersWithRole(teamAdminRole));
    var readerUserEmails = Lists.newArrayList(usersDao.listUsersWithRole(teamReaderRole));
    var writerUserEmails = Lists.newArrayList(usersDao.listUsersWithRole(teamWriterRole));
    return new ListTeamMembersResponse(
        teamId,
        new ArrayList<>(adminUserEmails),
        new ArrayList<>(writerUserEmails),
        new ArrayList<>(readerUserEmails));
  }
}
