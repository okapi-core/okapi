/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.web.service.dashboards;

import static org.junit.jupiter.api.Assertions.*;
import static org.okapi.web.auth.TestCommons.addToOrg;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.okapi.data.dao.UsersDao;
import org.okapi.web.auth.AbstractIT;
import org.okapi.web.auth.OrgManager;
import org.okapi.web.auth.TokenManager;
import org.okapi.web.auth.UserManager;
import org.okapi.web.auth.tx.GrantOrgEditToDashboard;
import org.okapi.web.dtos.auth.CreateUserRequest;
import org.okapi.web.dtos.auth.SignInRequest;
import org.okapi.web.dtos.dashboards.*;
import org.okapi.web.service.ProtectedResourceContext;
import org.okapi.web.service.context.DashboardAccessContext;
import org.okapi.web.service.dashboards.panels.DashboardPanelService;
import org.okapi.web.service.dashboards.rows.DashboardRowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Execution(ExecutionMode.CONCURRENT)
public class DashboardServiceIT extends AbstractIT {

  @Autowired UserManager userManager;
  @Autowired OrgManager orgManager;
  @Autowired TokenManager tokenManager;
  @Autowired UsersDao usersDao;

  @Autowired DashboardService dashboardService;
  @Autowired DashboardRowService rowService;
  @Autowired DashboardPanelService panelService;
  @Autowired org.okapi.data.dao.RelationGraphDao relationGraphDao;

  String adminEmail;
  String memberEmail;
  String outsiderEmail;

  String adminLogin;
  String memberLogin;
  String outsiderLogin;

  String adminTempToken;
  String memberTempToken;
  String outsiderTempToken;

  String orgId;

  @BeforeEach
  public void setupEach() throws Exception {
    super.setup();
    adminEmail = dedup("admin@dashservice.com", this.getClass());
    memberEmail = dedup("member@dashservice.com", this.getClass());
    outsiderEmail = dedup("outsider@dashservice.com", this.getClass());

    // create 3 users
    try {
      userManager.signupWithEmailPassword(new CreateUserRequest("Admin", "User", adminEmail, "pw"));
    } catch (Exception ignored) {
    }
    try {
      userManager.signupWithEmailPassword(
          new CreateUserRequest("Member", "User", memberEmail, "pw"));
    } catch (Exception ignored) {
    }
    try {
      userManager.signupWithEmailPassword(
          new CreateUserRequest("Out", "Sider", outsiderEmail, "pw"));
    } catch (Exception ignored) {
    }

    adminLogin = userManager.signInWithEmailPassword(new SignInRequest(adminEmail, "pw"));
    memberLogin = userManager.signInWithEmailPassword(new SignInRequest(memberEmail, "pw"));
    outsiderLogin = userManager.signInWithEmailPassword(new SignInRequest(outsiderEmail, "pw"));

    // get admin's org and add member to it
    var adminOrgs = orgManager.listOrgs(adminLogin);
    orgId = adminOrgs.getOrgs().get(0).getOrgId();
    addToOrg(usersDao, relationGraphDao, orgId, adminEmail, true);
    addToOrg(usersDao, relationGraphDao, orgId, memberEmail, false);

    adminTempToken = tokenManager.issueTemporaryToken(adminLogin, orgId);
    memberTempToken = tokenManager.issueTemporaryToken(memberLogin, orgId);

    // outsider is in a different org
    var outsiderOrgs = orgManager.listOrgs(outsiderLogin);
    var outsiderOrgId = outsiderOrgs.getOrgs().get(0).getOrgId();
    outsiderTempToken = tokenManager.issueTemporaryToken(outsiderLogin, outsiderOrgId);
  }

  @Test
  public void fullCycle_dashboard_rows_panels() throws Exception {
    // create dashboard
    var dash =
        dashboardService.create(
            DashboardAccessContext.of(adminTempToken),
            new CreateDashboardRequest("note", "title", List.of("tag1", "tag2")));
    var dashboardId = dash.getDashboardId();
    var versionId = dash.getActiveVersion();

    // grant org edit for ease of updates
    new GrantOrgEditToDashboard(orgId, dashboardId).doTx(relationGraphDao);

    // create 2 rows
    var row1 =
        rowService.create(
            ProtectedResourceContext.of(adminTempToken),
            CreateDashboardRowRequest.builder()
                .orgId(orgId)
                .dashboardId(dashboardId)
                .versionId(versionId)
                .rowId(UUID.randomUUID().toString())
                .title("Row 1")
                .description("Desc 1")
                .build());
    var row2 =
        rowService.create(
            ProtectedResourceContext.of(adminTempToken),
            CreateDashboardRowRequest.builder()
                .orgId(orgId)
                .dashboardId(dashboardId)
                .versionId(versionId)
                .rowId(UUID.randomUUID().toString())
                .title("Row 2")
                .description("Desc 2")
                .build());

    // add a panel to each row
    panelService.create(
        ProtectedResourceContext.of(adminTempToken),
        CreateDashboardPanelRequest.builder()
            .orgId(orgId)
            .dashboardId(dashboardId)
            .versionId(versionId)
            .rowId(row1.getRowId())
            .panelId("p1")
            .title("P1")
            .note("N1")
            .build());
    panelService.create(
        ProtectedResourceContext.of(adminTempToken),
        CreateDashboardPanelRequest.builder()
            .orgId(orgId)
            .dashboardId(dashboardId)
            .versionId(versionId)
            .rowId(row2.getRowId())
            .panelId("p2")
            .title("P2")
            .note("N2")
            .build());

    // read fat dashboard
    var fat = dashboardService.read(DashboardAccessContext.of(adminTempToken, dashboardId));
    assertEquals(2, fat.getRows().size());
    var panelsCount = fat.getRows().stream().map(r -> r.getPanels().size()).toList();
    assertEquals(List.of(1, 1), panelsCount);

    // check user details
    var creator = fat.getCreatedBy();
    assertEquals("Admin", creator.getFirstName());
    var editor = fat.getLastEditedBy();
    assertEquals("Admin", editor.getFirstName());
  }

  @Test
  public void updates_title_and_description() throws Exception {
    var dash =
        dashboardService.create(
            DashboardAccessContext.of(adminTempToken),
            new CreateDashboardRequest("note", "title", List.of("tag1", "tag2")));
    var dashboardId = dash.getDashboardId();
    dashboardService.update(
        DashboardAccessContext.of(adminTempToken, dashboardId),
        UpdateDashboardRequest.builder().desc("NewDesc").title("NewTitle").build());
    var after = dashboardService.read(DashboardAccessContext.of(adminTempToken, dashboardId));
    assertEquals("NewTitle", after.getTitle());
    assertEquals("NewDesc", after.getDescription());
  }

  @Test
  public void viewed_timestamps_updated_for_two_users() throws Exception {
    var dash =
        dashboardService.create(
            DashboardAccessContext.of(adminTempToken),
            new CreateDashboardRequest("note", "title", List.of("tag1")));
    var dashboardId = dash.getDashboardId();

    // First reads for each user should not yet show a viewed timestamp (set after read).
    var a1 = dashboardService.read(DashboardAccessContext.of(adminTempToken, dashboardId));
    var m1 = dashboardService.read(DashboardAccessContext.of(memberTempToken, dashboardId));
    assertNull(a1.getViewed());
    assertNull(m1.getViewed());

    // Second reads should expose the timestamp set by the previous read.
    var a2 = dashboardService.read(DashboardAccessContext.of(adminTempToken, dashboardId));
    var m2 = dashboardService.read(DashboardAccessContext.of(memberTempToken, dashboardId));
    assertNotNull(a2.getViewed());
    assertNotNull(m2.getViewed());

    // Third reads should advance timestamps; sleep briefly to ensure monotonicity.
    Thread.sleep(5);
    var a3 = dashboardService.read(DashboardAccessContext.of(adminTempToken, dashboardId));
    var m3 = dashboardService.read(DashboardAccessContext.of(memberTempToken, dashboardId));
    assertTrue(a3.getViewed().isAfter(a2.getViewed()));
    assertTrue(m3.getViewed().isAfter(m2.getViewed()));
  }

  @Test
  public void favorite_toggle_via_update_request() throws Exception {
    var dash =
        dashboardService.create(
            DashboardAccessContext.of(adminTempToken),
            new CreateDashboardRequest("note", "title", List.of("tag1")));
    var dashboardId = dash.getDashboardId();

    // Grant org edit so member can update dashboard
    new GrantOrgEditToDashboard(orgId, dashboardId).doTx(relationGraphDao);

    // Mark as favorite
    var afterFav =
        dashboardService.update(
            DashboardAccessContext.of(memberTempToken, dashboardId),
            UpdateDashboardRequest.builder().isFavorite(true).build());
    assertTrue(afterFav.isFavorite());

    // Unmark favorite
    var afterUnfav =
        dashboardService.update(
            DashboardAccessContext.of(memberTempToken, dashboardId),
            UpdateDashboardRequest.builder().isFavorite(false).build());
    assertFalse(afterUnfav.isFavorite());
  }
}
