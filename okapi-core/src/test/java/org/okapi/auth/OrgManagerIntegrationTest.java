package org.okapi.auth;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;
import static org.okapi.auth.TestCommons.addToOrg;

import com.okapi.rest.auth.AuthRequest;
import com.okapi.rest.org.CreateOrgMemberRequest;
import com.okapi.rest.org.DeleteOrgMemberRequest;
import com.okapi.rest.org.UpdateOrgRequest;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.okapi.exceptions.BadRequestException;
import org.okapi.exceptions.NotFoundException;
import org.okapi.exceptions.UnAuthorizedException;
import org.okapi.fixtures.Deduplicator;
import org.okapi.fixtures.SingletonFactory;

public class OrgManagerIntegrationTest {

  public static final String ORG_ID =
      Deduplicator.dedup("org-12345", OrgManagerIntegrationTest.class);
  UserManager userManager;
  SingletonFactory resourceFactory;

  String adminEmail = "admin_email_" + System.currentTimeMillis() + "@domain.com";
  String nonAdminEmail = "not_admin_email_" + System.currentTimeMillis() + "@domain.com";
  String adminTempToken;
  String nonAdminTempToken;

  @BeforeEach
  public void setup() throws BadRequestException, UnAuthorizedException {
    resourceFactory = new SingletonFactory();
    userManager = resourceFactory.getUserManager();

    // sign up first user
    for (var email : Arrays.asList(adminEmail, nonAdminEmail)) {
      var authRequest = new AuthRequest("Oscar", "Okapi", email, "password123");
      try {
        resourceFactory.getUserManager().signupWithEmailPassword(authRequest);
      } catch (Exception e) {
        System.out.println("User with email " + email + " already exists, skipping signup.");
      }
    }
    addToOrg(resourceFactory, ORG_ID, adminEmail, true);
    addToOrg(resourceFactory, ORG_ID, nonAdminEmail, false);
    adminTempToken =
        resourceFactory
            .tokenManager()
            .issueTemporaryBearerToken(
                ORG_ID,
                userManager.signInWithEmailPassword(
                    new AuthRequest("Oscar", "Okapi", adminEmail, "password123")));
    // sign up second user
    nonAdminTempToken =
        resourceFactory
            .tokenManager()
            .issueTemporaryBearerToken(
                ORG_ID,
                userManager.signInWithEmailPassword(
                    new AuthRequest("Oscar", "NotOKapi", nonAdminEmail, "password123")));
  }

  @Test
  public void testAddingAdminMember() throws UnAuthorizedException {
    var myOrgs = resourceFactory.orgManager().listOrgs(adminTempToken);
    var myOrg = myOrgs.getOrgs().get(0);
    resourceFactory
        .orgManager()
        .createOrgMember(
            adminTempToken, new CreateOrgMemberRequest(myOrg.getOrgId(), nonAdminEmail, true));

    var nonAdminOrgs = resourceFactory.orgManager().listOrgs(nonAdminTempToken);
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
    var myOrgs = resourceFactory.orgManager().listOrgs(adminTempToken);
    var myOrg = myOrgs.getOrgs().get(0);
    resourceFactory
        .orgManager()
        .createOrgMember(
            adminTempToken, new CreateOrgMemberRequest(myOrg.getOrgId(), nonAdminEmail, false));

    var nonAdminOrgs = resourceFactory.orgManager().listOrgs(nonAdminTempToken);
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
    var myOrgs = resourceFactory.orgManager().listOrgs(adminTempToken);
    var myOrg = myOrgs.getOrgs().get(0);

    // Add a member first
    resourceFactory
        .orgManager()
        .createOrgMember(
            adminTempToken, new CreateOrgMemberRequest(myOrg.getOrgId(), nonAdminEmail, false));

    // Now delete the member
    resourceFactory
        .orgManager()
        .deleteOrgMember(
            adminTempToken, new DeleteOrgMemberRequest(myOrg.getOrgId(), nonAdminEmail));

    // Verify the member is no longer listed in the org
    await()
        .atMost(Duration.of(1, ChronoUnit.SECONDS))
        .until(
            () -> {
              var nonAdminOrgs = resourceFactory.orgManager().listOrgs(nonAdminTempToken);
              return nonAdminOrgs.getOrgs().stream()
                  .noneMatch(org -> org.getOrgId().equals(myOrg.getOrgId()));
            });
  }

  @Test
  public void teUpdatingOrg() throws UnAuthorizedException, BadRequestException {
    var myorgs = resourceFactory.orgManager().listOrgs(adminTempToken);
    var myOrg = myorgs.getOrgs().get(0);
    resourceFactory
        .orgManager()
        .updateOrg(adminTempToken, myOrg.getOrgId(), new UpdateOrgRequest("New Org Name"));
    var updatedList = resourceFactory.orgManager().listOrgs(adminTempToken);
    var afterUpdate =
        updatedList.getOrgs().stream()
            .filter(org -> org.getOrgId().equals(myOrg.getOrgId()))
            .findFirst()
            .orElseThrow(() -> new UnAuthorizedException("Org not found after update"));
    assertEquals("New Org Name", afterUpdate.getOrgName(), "Org name should be updated");
  }

  @Test
  public void testUpdateFailsForNonAdmin() throws UnAuthorizedException {
    var adminOrgs = resourceFactory.orgManager().listOrgs(adminTempToken);
    var orgId = adminOrgs.getOrgs().get(0).getOrgId();
    resourceFactory
        .orgManager()
        .createOrgMember(adminTempToken, new CreateOrgMemberRequest(orgId, nonAdminEmail, false));
    assertThrows(
        UnAuthorizedException.class,
        () ->
            resourceFactory
                .orgManager()
                .updateOrg(nonAdminTempToken, orgId, new UpdateOrgRequest("New Org Name")));
  }
}
