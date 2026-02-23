package org.okapi.web.service.dashboards;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.okapi.web.auth.TestCommons.addToOrg;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.okapi.data.dao.DashboardDao;
import org.okapi.data.dao.DashboardVarDao;
import org.okapi.data.dao.RelationGraphDao;
import org.okapi.web.auth.AbstractIT;
import org.okapi.web.auth.AccessManager;
import org.okapi.web.auth.OrgManager;
import org.okapi.web.auth.TokenManager;
import org.okapi.web.auth.UserManager;
import org.okapi.web.auth.tx.GrantOrgEditToDashboard;
import org.okapi.web.dtos.auth.CreateUserRequest;
import org.okapi.web.dtos.auth.SignInRequest;
import org.okapi.web.dtos.dashboards.CreateDashboardRequest;
import org.okapi.web.dtos.dashboards.vars.CreateDashboardVarRequest;
import org.okapi.web.dtos.dashboards.vars.DASH_VAR_TYPE;
import org.okapi.web.dtos.dashboards.vars.DeleteDashboardVarRequest;
import org.okapi.web.service.context.DashboardAccessContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Execution(ExecutionMode.CONCURRENT)
public class DashboardVarsServiceIT extends AbstractIT {

  @Autowired UserManager userManager;
  @Autowired OrgManager orgManager;
  @Autowired TokenManager tokenManager;
  @Autowired AccessManager accessManager;
  @Autowired DashboardService dashboardService;
  @Autowired DashboardDao dashboardDao;
  @Autowired DashboardVarDao dashboardVarDao;
  @Autowired RelationGraphDao relationGraphDao;
  @Autowired org.okapi.data.dao.UsersDao usersDao;
  @Autowired DashboardVarsService service;

  String adminEmail;
  String adminLogin;
  String adminTempToken;
  String orgId;

  @BeforeEach
  public void setupEach() throws Exception {
    super.setup();
    adminEmail = dedup("admin@dashboardvars.com", this.getClass());

    try {
      userManager.signupWithEmailPassword(new CreateUserRequest("Admin", "User", adminEmail, "pw"));
    } catch (Exception ignored) {
    }
    adminLogin = userManager.signInWithEmailPassword(new SignInRequest(adminEmail, "pw"));
    var myOrgs = orgManager.listOrgs(adminLogin);
    orgId = myOrgs.getOrgs().get(0).getOrgId();
    addToOrg(usersDao, relationGraphDao, orgId, adminEmail, true);
    adminTempToken = tokenManager.issueTemporaryToken(adminLogin, orgId);
  }

  @Test
  public void fullCycle_dashboard_vars_create_list_delete() throws Exception {
    var dashRes =
        dashboardService.create(
            DashboardAccessContext.of(adminTempToken),
            new CreateDashboardRequest("dashNote", "dashTitle", List.of()));
    var dashboardId = dashRes.getDashboardId();
    new GrantOrgEditToDashboard(orgId, dashboardId).doTx(relationGraphDao);

    var v1Name = "var_" + UUID.randomUUID();
    var v2Name = "var_" + UUID.randomUUID();

    var v1 =
        service.createVar(
            adminTempToken,
            new CreateDashboardVarRequest(dashboardId, DASH_VAR_TYPE.METRIC, v1Name, "tag-1"));
    var v2 =
        service.createVar(
            adminTempToken,
            new CreateDashboardVarRequest(dashboardId, DASH_VAR_TYPE.TAG_VALUE, v2Name, "tag-2"));

    assertEquals(v1Name, v1.getName());
    assertEquals(DASH_VAR_TYPE.METRIC, v1.getType());
    assertEquals(v2Name, v2.getName());
    assertEquals(DASH_VAR_TYPE.TAG_VALUE, v2.getType());

    var listRes = service.listVar(adminTempToken, dashboardId);
    assertNotNull(listRes);
    assertNotNull(listRes.getVars());
    assertEquals(2, listRes.getVars().size());

    var byName = listRes.getVars().stream().collect(Collectors.toMap(v -> v.getName(), v -> v));
    assertTrue(byName.containsKey(v1Name));
    assertTrue(byName.containsKey(v2Name));
    assertEquals("tag-1", byName.get(v1Name).getTag());
    assertEquals("tag-2", byName.get(v2Name).getTag());

    service.deleteVar(adminTempToken, new DeleteDashboardVarRequest(dashboardId, v1Name));

    var listAfterDelete = service.listVar(adminTempToken, dashboardId);
    assertEquals(1, listAfterDelete.getVars().size());
    assertTrue(listAfterDelete.getVars().stream().anyMatch(v -> v2Name.equals(v.getName())));
  }
}
