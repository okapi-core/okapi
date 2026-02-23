package org.okapi.web.auth;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;
import static org.okapi.fixtures.Deduplicator.dedup;
import static org.okapi.web.auth.TestCommons.addToOrg;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.okapi.data.dao.RelationGraphDao;
import org.okapi.data.dao.UsersDao;
import org.okapi.exceptions.BadRequestException;
import org.okapi.exceptions.NotFoundException;
import org.okapi.exceptions.UnAuthorizedException;
import org.okapi.web.dtos.auth.CreateUserRequest;
import org.okapi.web.dtos.auth.SignInRequest;
import org.okapi.web.dtos.org.CreateOrgMemberRequest;
import org.okapi.web.dtos.org.UpdateOrgMemberRequest;
import org.okapi.web.dtos.org.UpdateOrgRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
@ActiveProfiles("test")
public class OrgManagerIT extends AbstractIT {
  static final String ORG_ID = "org-it-" + UUID.randomUUID();

  @DynamicPropertySource
  static void registerOrgId(DynamicPropertyRegistry registry) {
    registry.add("orgId", () -> ORG_ID);
  }

  String adminEmail = dedup("admin@domain.com", this.getClass());
  String nonAdminEmail = dedup("not_admin@domain.com", this.getClass());
  String adminTempToken;
  String nonAdminTempToken;

  @Autowired UserManager userManager;
  @Autowired UsersDao usersDao;
  @Autowired TokenManager tokenManager;
  @Autowired OrgManager orgManager;
  @Autowired RelationGraphDao relationGraphDao;

  String orgId;

  @BeforeEach
  public void setup() throws UnAuthorizedException {
    super.setup();
    // sign up first user
    for (var email : Arrays.asList(adminEmail, nonAdminEmail)) {
      var authRequest = new CreateUserRequest("Oscar", "Okapi", email, "password123");
      try {
        userManager.signupWithEmailPassword(authRequest);
      } catch (Exception e) {
        System.out.println("User with email " + email + " already exists, skipping signup.");
      }
    }
    var adminLoginToken =
        userManager.signInWithEmailPassword(new SignInRequest(adminEmail, "password123"));
    var userOrg = orgManager.listOrgs(adminLoginToken);
    orgId = userOrg.getOrgs().get(0).getOrgId();
    addToOrg(usersDao, relationGraphDao, orgId, adminEmail, true);
    addToOrg(usersDao, relationGraphDao, orgId, nonAdminEmail, false);
    adminTempToken =
        tokenManager.issueTemporaryToken(
            userManager.signInWithEmailPassword(new SignInRequest(adminEmail, "password123")),
            orgId);
    // sign up second user
    nonAdminTempToken =
        tokenManager.issueTemporaryToken(
            userManager.signInWithEmailPassword(new SignInRequest(nonAdminEmail, "password123")),
            orgId);
  }

  @Test
  public void testAddingAdminMember() throws UnAuthorizedException {
    var myOrgs = orgManager.listOrgs(adminTempToken);
    var myOrg = myOrgs.getOrgs().get(0);
    orgManager.createOrgMember(
        adminTempToken, new CreateOrgMemberRequest(myOrg.getOrgId(), nonAdminEmail, true));

    var nonAdminOrgs = orgManager.listOrgs(nonAdminTempToken);
    var nonAdminOrg =
        nonAdminOrgs.getOrgs().stream()
            .filter(org -> org.getOrgId().equals(myOrg.getOrgId()))
            .findFirst()
            .orElseThrow(
                () -> new UnAuthorizedException("Non-admin user does not have access to the org"));
    assertEquals(
        myOrg.getOrgId(),
        nonAdminOrg.getOrgId(),
        "Non-admin user should have access to the org they were added to as an admin");
  }

  @Test
  public void testAddingNonAdminMember() throws UnAuthorizedException {
    var myOrgs = orgManager.listOrgs(adminTempToken);
    var myOrg = myOrgs.getOrgs().get(0);
    orgManager.createOrgMember(
        adminTempToken, new CreateOrgMemberRequest(myOrg.getOrgId(), nonAdminEmail, false));

    var nonAdminOrgs = orgManager.listOrgs(nonAdminTempToken);
    var nonAdminOrg =
        nonAdminOrgs.getOrgs().stream()
            .filter(org -> org.getOrgId().equals(myOrg.getOrgId()))
            .findFirst()
            .orElseThrow(
                () -> new UnAuthorizedException("Non-admin user does not have access to the org"));
    assertEquals(
        myOrg.getOrgId(),
        nonAdminOrg.getOrgId(),
        "Non-admin user should have access to the org they were added to as a member");
  }

  @Test
  public void testDeletingOrgMember()
      throws UnAuthorizedException, NotFoundException, BadRequestException {
    var myOrgs = orgManager.listOrgs(adminTempToken);
    var myOrg = myOrgs.getOrgs().get(0);

    // Add a member first
    orgManager.createOrgMember(
        adminTempToken, new CreateOrgMemberRequest(myOrg.getOrgId(), nonAdminEmail, false));

    // Now delete the member
    orgManager.updateOrgMember(
        adminTempToken, myOrg.getOrgId(), new UpdateOrgMemberRequest(nonAdminEmail, List.of()));

    // Verify the member is no longer listed in the org
    await()
        .atMost(Duration.of(1, ChronoUnit.SECONDS))
        .until(
            () -> {
              var nonAdminOrgs = orgManager.listOrgs(nonAdminTempToken);
              return nonAdminOrgs.getOrgs().stream()
                  .noneMatch(org -> org.getOrgId().equals(myOrg.getOrgId()));
            });
  }

  @Test
  public void testUpdatingOrg() throws UnAuthorizedException, BadRequestException {
    var myorgs = orgManager.listOrgs(adminTempToken);
    var myOrg = myorgs.getOrgs().get(0);
    orgManager.updateOrg(adminTempToken, myOrg.getOrgId(), new UpdateOrgRequest("New Org Name"));
    var updatedList = orgManager.listOrgs(adminTempToken);
    var afterUpdate =
        updatedList.getOrgs().stream()
            .filter(org -> org.getOrgId().equals(myOrg.getOrgId()))
            .findFirst()
            .orElseThrow(() -> new UnAuthorizedException("Org not found after update"));
    assertEquals("New Org Name", afterUpdate.getOrgName(), "Org name should be updated");
  }
}
