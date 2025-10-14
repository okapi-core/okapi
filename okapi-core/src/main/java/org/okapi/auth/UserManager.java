package org.okapi.auth;

import static org.okapi.validation.OkapiChecks.checkArgument;

import com.google.common.collect.Lists;
import java.util.*;
import lombok.AllArgsConstructor;
import org.okapi.bcrypt.BCrypt;
import org.okapi.data.Mappers;
import org.okapi.data.dao.OrgDao;
import org.okapi.data.dao.UsersDao;
import org.okapi.data.dto.OrgDto;
import org.okapi.data.dto.UserDto;
import org.okapi.data.exceptions.UserAlreadyExistsException;
import org.okapi.exceptions.BadRequestException;
import org.okapi.exceptions.NotFoundException;
import org.okapi.exceptions.UnAuthorizedException;
import org.okapi.metrics.IdCreationFailedException;
import org.okapi.metrics.IdCreator;
import org.okapi.rest.auth.AuthRequest;
import org.okapi.rest.org.ListOrgsResponse;
import org.okapi.rest.team.CreateTeamRequest;
import org.okapi.rest.users.GetOrgUserView;
import org.okapi.rest.users.GetUserMetadataResponse;
import org.okapi.usermessages.UserFacingMessages;

@AllArgsConstructor
public class UserManager {
  private final UsersDao usersDao;
  private final OrgDao orgDao;
  private final TokenManager tokenManager;
  private final TeamsManager teamsManager;

  public String signupWithEmailPassword(AuthRequest request)
      throws BadRequestException,
          UserAlreadyExistsException,
          IdCreationFailedException,
          UnAuthorizedException {
    if (request.getPassword() == null) {
      throw new BadRequestException(UserFacingMessages.NO_PASSWORD);
    }
    try {
      var user =
          usersDao.create(
              request.getFirstName(),
              request.getLastName(),
              request.getEmail(),
              request.getPassword());
      afterSignUp(user.getUserId());
      var loginToken = tokenManager.issueLoginToken(user.getUserId());
      return loginToken;
    } catch (UserAlreadyExistsException e) {
      throw new BadRequestException(UserFacingMessages.USER_ALREADY_EXISTS);
    }
  }

  public String signInWithEmailPassword(AuthRequest request) throws UnAuthorizedException {
    var user = usersDao.getWithEmail(request.getEmail());
    if (user.isEmpty()) {
      throw new UnAuthorizedException();
    }

    var passwordMatch = BCrypt.checkpw(request.getPassword(), user.get().getHashedPassword());
    if (!passwordMatch) {
      throw new UnAuthorizedException();
    }

    var userIsActive = user.get().getStatus() == UserDto.UserStatus.ACTIVE;
    if (!userIsActive) {
      throw new UnAuthorizedException();
    }
    return tokenManager.issueLoginToken(user.get().getUserId());
  }

  public ListOrgsResponse listUserOrgs(String token) throws UnAuthorizedException {
    var userId = tokenManager.getUserId(token);
    var orgs = listUserOrgsAux(userId);
    return ListOrgsResponse.builder().orgs(orgs).build();
  }

  private List<GetOrgUserView> listUserOrgsAux(String userId) {
    var userRoles = Lists.newArrayList(usersDao.listRolesByUserId(userId));
    var orgs = new ArrayList<GetOrgUserView>();
    for (var role : userRoles) {
      // these are either admin or member
      var parsed = RoleTemplates.parseAsOrgRole(role.getRole());
      if (parsed.isEmpty()) continue;
      var org = orgDao.findById(parsed.get().orgId());
      if (org.isEmpty()) continue;
      var creator = org.get().getOrgCreator().equals(userId);
      orgs.add(
          GetOrgUserView.builder()
              .isCreator(creator)
              .orgId(org.get().getOrgId())
              .orgName(org.get().getOrgName())
              .build());
    }

    return orgs;
  }

  private void afterSignUp(String userId) throws UnAuthorizedException, IdCreationFailedException {
    var orgDto = createUniqueOrgForUser(userId);
    makeUserAdmin(userId, orgDto.getOrgId());
    var loginToken = tokenManager.issueLoginToken(userId);
    var tempToken = tokenManager.issueTemporaryBearerToken(orgDto.getOrgId(), loginToken);
    var teamName = orgDto.getOrgName() + " Team";
    var createTeamRequest =
        CreateTeamRequest.builder()
            .name(teamName)
            .description("Default team for " + orgDto.getOrgName())
            .build();
    teamsManager.createTeam(tempToken, createTeamRequest);
  }

  private void makeUserAdmin(String userId, String orgId) {
    usersDao.grantRole(userId, RoleTemplates.getOrgAdminRole(orgId));
    usersDao.grantRole(userId, RoleTemplates.getOrgMemberRole(orgId));
  }

  private OrgDto createUniqueOrgForUser(String userId) throws IdCreationFailedException {
    var user = usersDao.get(userId).get();
    var orgName = "Default Org";

    if (user.getFirstName() != null) {
      orgName = user.getFirstName() + "'s Org";
    }

    var orgId =
        IdCreator.generateTeamId(
            id -> {
              var loaded = orgDao.findById(id);
              return loaded.isPresent();
            });
    var orgDto = OrgDto.builder().orgId(orgId).orgCreator(userId).orgName(orgName).build();
    orgDao.save(orgDto);
    return orgDto;
  }

  public GetUserMetadataResponse getUserMetadata(String token)
      throws UnAuthorizedException, NotFoundException {
    var userId = tokenManager.getUserId(token);
    var user = usersDao.get(userId);
    checkArgument(user.isPresent(), NotFoundException::new);
    var orgs = listUserOrgs(token);
    return Mappers.buildUserMetadataResponse(orgs.getOrgs(), user.get());
  }
}
