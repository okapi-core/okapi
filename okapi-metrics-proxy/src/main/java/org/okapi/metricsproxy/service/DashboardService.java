package org.okapi.metricsproxy.service;

import static org.okapi.data.dao.RelationGraphDao.makeRelation;
import static org.okapi.data.dto.RelationGraphNode.ENTITY_TYPE.*;
import static org.okapi.data.dto.RelationGraphNode.RELATION_TYPE.*;
import static org.okapi.data.dto.RelationGraphNode.makeEntityId;
import static org.okapi.validation.OkapiChecks.checkArgument;

import org.okapi.rest.dashboards.CreateDashboardRequest;
import org.okapi.rest.dashboards.GetDashboardResponse;
import org.okapi.rest.dashboards.UpdateDashboardRequest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.okapi.auth.AccessManager;
import org.okapi.auth.TokenManager;
import org.okapi.data.Mappers;
import org.okapi.data.dao.CommonGraphWalks;
import org.okapi.data.dao.DashboardDao;
import org.okapi.data.dao.RelationGraphDao;
import org.okapi.data.dao.TeamsDao;
import org.okapi.data.dto.DashboardDto;
import org.okapi.data.dto.RelationGraphNode;
import org.okapi.data.exceptions.ResourceNotFoundException;
import org.okapi.exceptions.UnAuthorizedException;

@AllArgsConstructor
public class DashboardService {

  DashboardDao dashboardDao;
  TokenManager tokenManager;
  AccessManager accessManager;
  CommonGraphWalks commonGraphWalks;
  RelationGraphDao relationGraphDao;
  TeamsDao teamsDao;

  public GetDashboardResponse create(String tempHeader, CreateDashboardRequest request)
      throws UnAuthorizedException, ResourceNotFoundException {
    var userId = tokenManager.getUserId(tempHeader);
    var orgId = tokenManager.getOrgId(tempHeader);
    accessManager.checkUserHasIsOrgMember(userId, orgId);
    var id = UUID.randomUUID().toString();
    var newDto =
        DashboardDto.builder()
            .dashboardId(id)
            .orgId(orgId)
            .creator(userId)
            .dashboardNote(request.getNote())
            .lastEditor(userId)
            .dashboardTitle(request.getTitle())
            .dashboardStatus(DashboardDto.DASHBOARD_STATUS.ACTIVE)
            .build();
    dashboardDao.save(newDto);
    dashboardDao.updateDefinition(id, request.getDefinition());
    processOrgWideAccess(userId, orgId, id, request);
    processTeamWideAccess(userId, orgId, id, request);
    return Mappers.mapDashboardDtoToResponse(request.getDefinition(), newDto);
  }

  public GetDashboardResponse get(String tempHeader, String id)
      throws UnAuthorizedException, ResourceNotFoundException {
    var userId = tokenManager.getUserId(tempHeader);
    var orgId = tokenManager.getOrgId(tempHeader);
    var dto = dashboardDao.get(id);
    checkArgument(dto.isPresent(), ResourceNotFoundException::new);
    var definition = dashboardDao.getDefinition(id);
    return Mappers.mapDashboardDtoToResponse(definition, dto.get());
  }

  public void update(String tempHeader, String dashboardId, UpdateDashboardRequest request)
      throws UnAuthorizedException, ResourceNotFoundException {
    var userId = tokenManager.getUserId(tempHeader);
    var editPathways =
        Arrays.asList(
            new RelationGraphDao.EdgeSeq(
                Arrays.asList(
                    makeRelation(ORG, ORG_MEMBER), makeRelation(DASHBOARD, DASHBOARD_EDIT))),
            new RelationGraphDao.EdgeSeq(
                Arrays.asList(
                    makeRelation(TEAM, TEAM_MEMBER), makeRelation(DASHBOARD, DASHBOARD_EDIT))));
    var userEntity = makeEntityId(USER, userId);
    var dashboardEntity = makeEntityId(DASHBOARD, dashboardId);
    var canEdit = relationGraphDao.aPathExists(userEntity, dashboardEntity, editPathways);
    checkArgument(canEdit, UnAuthorizedException::new);
    var dashboardDtoOptional = dashboardDao.get(dashboardId);
    checkArgument(dashboardDtoOptional.isPresent(), ResourceNotFoundException::new);
    if (request.getDefinition() != null) {
      dashboardDao.updateDefinition(dashboardId, request.getDefinition());
    }

    var dto = dashboardDtoOptional.get();
    var wasUpdated = false;
    if (request.getTitle() != null) {
      dto.setDashboardTitle(request.getTitle());
      wasUpdated = true;
    }
    if (request.getNote() != null) {
      dto.setDashboardNote(request.getNote());
      wasUpdated = true;
    }
    if (wasUpdated) {
      dto.setLastEditor(userId);
    }
    dashboardDao.save(dto);
  }

  private final void processOrgWideAccess(
      String userId, String orgId, String dashboardId, CreateDashboardRequest request)
      throws UnAuthorizedException {
    var userIsAMember = commonGraphWalks.userIsOrgMember(userId, orgId);
    checkArgument(userIsAMember, UnAuthorizedException::new);
    var relations = new ArrayList<RelationGraphNode.RELATION_TYPE>();
    if (request.isOrgWideWrite()) relations.add(RelationGraphNode.RELATION_TYPE.DASHBOARD_READ);
    if (request.isOrgWideWrite()) relations.add(RelationGraphNode.RELATION_TYPE.DASHBOARD_EDIT);
    relationGraphDao.addAll(
        makeEntityId(ORG, orgId), makeEntityId(DASHBOARD, dashboardId), relations);
  }

  private final void processTeamWideAccess(
      String userId, String orgId, String dashboardId, CreateDashboardRequest request)
      throws UnAuthorizedException {

    var toProcess = new HashSet<String>();
    toProcess.addAll(request.getTeamsWithReadAccess());
    toProcess.addAll(request.getTeamsWithWriteAccess());
    for (var team : toProcess) {
      var teamDtoOptional = teamsDao.get(team);
      checkArgument(teamDtoOptional.isPresent(), UnAuthorizedException::new);
      var org = teamDtoOptional.get().getOrgId();
      checkArgument(org.equals(orgId), UnAuthorizedException::new);
      var relations = new ArrayList<RelationGraphNode.RELATION_TYPE>();
      if (request.getTeamsWithWriteAccess().contains(team)) {
        relations.add(DASHBOARD_EDIT);
      }
      if (request.getTeamsWithReadAccess().contains(team)) {
        relations.add(DASHBOARD_READ);
      }
      relationGraphDao.addAll(
          makeEntityId(TEAM, team), makeEntityId(DASHBOARD, dashboardId), relations);
    }
  }
}
