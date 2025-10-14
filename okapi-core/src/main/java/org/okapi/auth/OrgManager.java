package org.okapi.auth;

import static org.okapi.data.dto.RelationGraphNode.ENTITY_TYPE.ORG;
import static org.okapi.data.dto.RelationGraphNode.ENTITY_TYPE.USER;
import static org.okapi.data.dto.RelationGraphNode.makeEntityId;
import static org.okapi.validation.OkapiChecks.checkArgument;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import java.util.*;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.okapi.data.dao.OrgDao;
import org.okapi.data.dao.RelationGraphDao;
import org.okapi.data.dao.UsersDao;
import org.okapi.data.dto.RelationGraphNode;
import org.okapi.exceptions.BadRequestException;
import org.okapi.exceptions.NotFoundException;
import org.okapi.exceptions.UnAuthorizedException;
import org.okapi.rest.org.*;
import org.okapi.rest.users.GetOrgUserView;
import org.okapi.usermessages.UserFacingMessages;

@AllArgsConstructor
public class OrgManager {
  TokenManager tokenManager;
  AccessManager accessManager;
  UsersDao usersDao;
  OrgDao orgDao;
  RelationGraphDao relationGraphDao;

  public void createOrgMember(String tempToken, CreateOrgMemberRequest request)
      throws UnAuthorizedException {
    var userId = tokenManager.getUserId(tempToken);
    var orgId = request.getOrgId();
    accessManager.checkUserHasIsOrgAdmin(userId, orgId);
    var memberUser = usersDao.getWithEmail(request.getEmail());
    if (memberUser.isEmpty()) {
      throw new UnAuthorizedException(UserFacingMessages.USER_NOT_FOUND);
    }
    var userDto = memberUser.get();
    // member role is always granted
    usersDao.grantRole(userDto.getUserId(), RoleTemplates.getOrgMemberRole(orgId));
    relationGraphDao.add(
        makeEntityId(USER, userDto.getUserId()),
        makeEntityId(ORG, orgId),
        RelationGraphNode.RELATION_TYPE.ORG_MEMBER);

    if (request.isAdmin()) {
      // if the request allows for an admin, the user is optionally granted an Admin role.
      usersDao.grantRole(userDto.getUserId(), RoleTemplates.getOrgAdminRole(orgId));
      relationGraphDao.add(
          makeEntityId(USER, userDto.getUserId()),
          makeEntityId(ORG, orgId),
          RelationGraphNode.RELATION_TYPE.ORG_ADMIN);
    }
  }

  public void deleteOrgMember(String tempToken, DeleteOrgMemberRequest request)
      throws UnAuthorizedException, NotFoundException, BadRequestException {
    var userId = tokenManager.getUserId(tempToken);
    var orgId = request.getOrgId();
    var memberEmail = request.getEmail();
    accessManager.checkUserHasIsOrgAdmin(userId, orgId);
    var org = orgDao.findById(orgId);
    checkArgument(org.isPresent(), NotFoundException::new);
    var memberUser = usersDao.getWithEmail(memberEmail);
    checkArgument(
        memberUser.isPresent(), () -> new BadRequestException(UserFacingMessages.USER_NOT_FOUND));
    checkArgument(
        !org.get().getOrgCreator().equals(memberUser.get().getUserId()),
        () -> new BadRequestException(UserFacingMessages.CANT_REMOVE_SELF));

    var orgMemberRole = RoleTemplates.getOrgMemberRole(orgId);
    var orgAdminRole = RoleTemplates.getOrgAdminRole(orgId);
    var memberId = memberUser.get().getUserId();
    relationGraphDao.removeAll(makeEntityId(USER, userId), makeEntityId(ORG, orgId));
    usersDao.revokeRole(memberId, orgMemberRole);
    usersDao.revokeRole(memberId, orgAdminRole);
  }

  public GetOrgResponse getOrg(String tempToken, String orgId) throws UnAuthorizedException {
    var userId = tokenManager.getUserId(tempToken);
    accessManager.checkUserHasIsOrgMember(userId, orgId);
    var members =
        Lists.newArrayList(usersDao.listUsersWithRole(RoleTemplates.getOrgMemberRole(orgId)));
    var admins =
        Lists.newArrayList(usersDao.listUsersWithRole(RoleTemplates.getOrgAdminRole(orgId)));
    var maybeOrg =
        orgDao
            .findById(orgId)
            .map(dto -> new GetOrgResponse(dto.getOrgId(), dto.getOrgName(), members, admins))
            .orElseThrow(() -> new UnAuthorizedException(UserFacingMessages.ORG_NOT_FOUND));
    return maybeOrg;
  }

  // list all orgs that a user is a member of
  public ListOrgsResponse listOrgs(String loginToken) throws UnAuthorizedException {
    var userId = tokenManager.getUserId(loginToken);
    var roles = Lists.newArrayList(usersDao.listRolesByUserId(userId));
    var orgsOfThisUser = new HashMap<String, GetOrgUserView.GetOrgUserViewBuilder>();
    var rolesPerOrg = ArrayListMultimap.<String, RoleTemplates.ORG_ROLE>create();

    for (var role : roles) {
      var parsed = RoleTemplates.parseAsOrgRole(role.getRole());
      if (parsed.isEmpty()) continue;
      var orgId = parsed.get().orgId();
      var org = orgDao.findById(orgId);
      if (org.isEmpty()) continue;
      rolesPerOrg.put(orgId, parsed.get().orgRole());
      if (!orgsOfThisUser.containsKey(orgId)) {
        orgsOfThisUser.put(
            orgId,
            GetOrgUserView.builder()
                .isCreator(userId.equals(org.get().getOrgCreator()))
                .orgId(org.get().getOrgId())
                .orgName(org.get().getOrgName()));
      }
    }

    var orgs = new ArrayList<GetOrgUserView>();
    for (var entry : orgsOfThisUser.entrySet()) {
      var orgBuilder = entry.getValue();
      var orgId = entry.getKey();
      var orgRoles =
          rolesPerOrg.get(orgId).stream()
              .map(
                  role ->
                      switch (role) {
                        case ADMIN -> "admin";
                        case MEMBER -> "member";
                      })
              .collect(Collectors.toSet());
      orgs.add(orgBuilder.roles(Lists.newArrayList(orgRoles)).build());
    }

    return ListOrgsResponse.builder().orgs(orgs).build();
  }

  public GetOrgResponse updateOrg(String tempToken, String orgId, UpdateOrgRequest request)
      throws UnAuthorizedException, BadRequestException {
    var userId = tokenManager.getUserId(tempToken);
    accessManager.checkUserHasIsOrgAdmin(userId, orgId);
    var maybeOrgDto = orgDao.findById(orgId);
    maybeOrgDto.orElseThrow(() -> new BadRequestException(UserFacingMessages.ORG_NOT_FOUND));
    var orgDto = maybeOrgDto.get();
    orgDto.setOrgName(request.getOrgName());
    orgDao.save(orgDto);
    return GetOrgResponse.builder().orgId(orgId).orgName(orgDto.getOrgName()).build();
  }
}
