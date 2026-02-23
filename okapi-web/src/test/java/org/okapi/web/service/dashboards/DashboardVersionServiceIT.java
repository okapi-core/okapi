package org.okapi.web.service.dashboards;

import static org.junit.jupiter.api.Assertions.*;
import static org.okapi.web.auth.TestCommons.addToOrg;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.okapi.data.dao.DashboardDao;
import org.okapi.data.dao.DashboardVersionDao;
import org.okapi.web.auth.AbstractIT;
import org.okapi.web.auth.OrgManager;
import org.okapi.web.auth.TokenManager;
import org.okapi.web.auth.UserManager;
import org.okapi.web.dtos.auth.CreateUserRequest;
import org.okapi.web.dtos.auth.SignInRequest;
import org.okapi.web.dtos.dashboards.yaml.ApplyDashboardYamlRequest;
import org.okapi.web.service.dashboards.DashboardVersionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
public class DashboardVersionServiceIT extends AbstractIT {

  @Autowired UserManager userManager;
  @Autowired OrgManager orgManager;
  @Autowired TokenManager tokenManager;
  @Autowired DashboardYamlIngestionService ingestionService;
  @Autowired DashboardVersionService versionService;
  @Autowired DashboardDao dashboardDao;
  @Autowired DashboardVersionDao dashboardVersionDao;
  @Autowired org.okapi.data.dao.UsersDao usersDao;
  @Autowired org.okapi.data.dao.RelationGraphDao relationGraphDao;

  String adminEmail;
  String adminLogin;
  String adminTempToken;
  String orgId;

  @BeforeEach
  public void setupEach() throws Exception {
    super.setup();
    adminEmail = dedup("admin@dashversion.com", this.getClass());
    try {
      userManager.signupWithEmailPassword(new CreateUserRequest("Admin", "User", adminEmail, "pw"));
    } catch (Exception ignored) {
    }
    adminLogin = userManager.signInWithEmailPassword(new SignInRequest(adminEmail, "pw"));
    orgId = orgManager.listOrgs(adminLogin).getOrgs().get(0).getOrgId();
    addToOrg(usersDao, relationGraphDao, orgId, adminEmail, true);
    adminTempToken = tokenManager.issueTemporaryToken(adminLogin, orgId);
  }

  @Test
  public void publish_updates_active_version() throws Exception {
    var dashboardId = "dash-" + UUID.randomUUID();
    var yaml =
        """
        version: 1
        dashboard:
          id: "%s"
          title: "Test Dash"
          description: "Test"
          rows:
            - id: "row-1"
              title: "Row 1"
              panels:
                - id: "panel-1"
                  title: "Panel 1"
                  queries:
                    - query: "{\\"svc\\":\\"svc\\",\\"metric\\":\\"m\\",\\"tags\\":{},\\"start\\":0,\\"end\\":0,\\"metricType\\":\\"GAUGE\\"}"
        """
            .formatted(dashboardId);

    var apply =
        ingestionService.apply(
            adminTempToken, ApplyDashboardYamlRequest.builder().yaml(yaml).build());
    assertTrue(apply.isOk());

    var publish =
        versionService.publish(adminTempToken, dashboardId, apply.getVersionId());
    assertEquals("PUBLISHED", publish.getStatus());

    var dash = dashboardDao.get(orgId, dashboardId).get();
    assertEquals(apply.getVersionId(), dash.getActiveVersion());

    var versionOpt = dashboardVersionDao.get(orgId, dashboardId, apply.getVersionId());
    assertTrue(versionOpt.isPresent());
    assertEquals("PUBLISHED", versionOpt.get().getStatus());
  }
}
