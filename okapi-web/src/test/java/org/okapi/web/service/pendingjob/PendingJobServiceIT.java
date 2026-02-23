package org.okapi.web.service.pendingjob;

import static org.junit.jupiter.api.Assertions.*;
import static org.okapi.web.auth.TestCommons.addToOrg;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.okapi.agent.dto.QueryResult;
import org.okapi.data.dao.PendingJobsDao;
import org.okapi.data.dao.RelationGraphDao;
import org.okapi.data.dao.UsersDao;
import org.okapi.data.dto.DataSourceQuery;
import org.okapi.data.dto.JOB_STATUS;
import org.okapi.data.dto.PendingJobDdb;
import org.okapi.exceptions.BadRequestException;
import org.okapi.web.auth.AbstractIT;
import org.okapi.web.auth.ApiTokenManager;
import org.okapi.web.auth.OrgManager;
import org.okapi.web.auth.UserManager;
import org.okapi.web.dtos.auth.CreateUserRequest;
import org.okapi.web.dtos.auth.SignInRequest;
import org.okapi.web.dtos.pendingjob.GetPendingJobsRequest;
import org.okapi.web.dtos.pendingjob.UpdatePendingJobRequest;
import org.okapi.web.service.token.Permissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class PendingJobServiceIT extends AbstractIT {

  @Autowired private PendingJobService pendingJobService;
  @Autowired private PendingJobsDao pendingJobsDao;
  @Autowired private ApiTokenManager apiTokenManager;
  @Autowired private UserManager userManager;
  @Autowired private OrgManager orgManager;
  @Autowired private UsersDao usersDao;
  @Autowired private RelationGraphDao relationGraphDao;

  private String orgId;
  private String authHeader;
  private String jobSource;

  @BeforeEach
  void setupEach() throws Exception {
    super.setup();
    var email = dedup("pendingjob@test.com", this.getClass());
    var password = "pw";
    try {
      userManager.signupWithEmailPassword(
          new CreateUserRequest("Pending", "User", email, password));
    } catch (Exception ignored) {
    }
    var loginToken = userManager.signInWithEmailPassword(new SignInRequest(email, password));
    orgId = orgManager.listOrgs(loginToken).getOrgs().get(0).getOrgId();
    addToOrg(usersDao, relationGraphDao, orgId, email, true);
    jobSource = "source-" + UUID.randomUUID();

    var bearer =
        apiTokenManager.createApiToken(
            orgId, List.of(Permissions.AGENT_JOBS_READ, Permissions.AGENT_JOBS_UPDATE));
    authHeader = "Bearer " + bearer;
  }

  @Test
  void submit_result_marks_job_completed() throws Exception {
    var jobId = UUID.randomUUID().toString();
    var pendingJob =
        PendingJobDdb.builder()
            .orgId(orgId)
            .jobId(jobId)
            .jobStatus(JOB_STATUS.PENDING)
            .sourceId(jobSource)
            .query(new DataSourceQuery("query", jobSource))
            .attemptCount(0)
            .createdAt(System.currentTimeMillis())
            .orgSourceStatusKey(
                PendingJobDdb.buildOrgSourceStatusKey(orgId, jobSource, JOB_STATUS.PENDING))
            .build();
    pendingJobsDao.createPendingJob(pendingJob);

    var pending =
        pendingJobService.getPendingJobs(authHeader, new GetPendingJobsRequest(List.of(jobSource)));
    assertEquals(1, pending.getPendingJobs().size());
    assertEquals(jobId, pending.getPendingJobs().get(0).getJobId());

    moveToInProgress(jobId);

    var result = QueryResult.ofData("ok");
    pendingJobService.submitJobResult(authHeader, jobId, result);

    var stored = pendingJobsDao.getPendingJob(orgId, jobId).orElseThrow();
    assertEquals(JOB_STATUS.COMPLETED, stored.getJobStatus());
  }

  @Test
  void cancel_after_completion_fails() throws Exception {
    var jobId = UUID.randomUUID().toString();
    var pendingJob =
        PendingJobDdb.builder()
            .orgId(orgId)
            .jobId(jobId)
            .jobStatus(JOB_STATUS.PENDING)
            .sourceId(jobSource)
            .query(new DataSourceQuery("query", jobSource))
            .attemptCount(0)
            .createdAt(System.currentTimeMillis())
            .orgSourceStatusKey(
                PendingJobDdb.buildOrgSourceStatusKey(orgId, jobSource, JOB_STATUS.PENDING))
            .build();
    pendingJobsDao.createPendingJob(pendingJob);

    moveToInProgress(jobId);

    pendingJobService.submitJobResult(authHeader, jobId, QueryResult.ofData("done"));

    assertThrows(
        BadRequestException.class,
        () ->
            pendingJobService.updatePendingJob(
                authHeader,
                new UpdatePendingJobRequest(jobId, org.okapi.agent.dto.JOB_STATUS.CANCELED)));
  }

  @Test
  void retry_failed_job_becomes_pending_again() throws Exception {
    var jobId = UUID.randomUUID().toString();
    var pendingJob =
        PendingJobDdb.builder()
            .orgId(orgId)
            .jobId(jobId)
            .jobStatus(JOB_STATUS.PENDING)
            .sourceId(jobSource)
            .query(new DataSourceQuery("retry-query", jobSource))
            .attemptCount(0)
            .createdAt(System.currentTimeMillis())
            .orgSourceStatusKey(
                PendingJobDdb.buildOrgSourceStatusKey(orgId, jobSource, JOB_STATUS.PENDING))
            .build();
    pendingJobsDao.createPendingJob(pendingJob);

    pendingJobService.updatePendingJob(
        authHeader, new UpdatePendingJobRequest(jobId, org.okapi.agent.dto.JOB_STATUS.IN_PROGRESS));
    pendingJobService.submitPendingJobError(authHeader, jobId, QueryResult.ofError("failed"));

    var failed = pendingJobsDao.getPendingJob(orgId, jobId).orElseThrow();
    assertEquals(JOB_STATUS.FAILED, failed.getJobStatus());

    pendingJobsDao.retryJob(orgId, jobId);

    var pending =
        pendingJobService.getPendingJobs(authHeader, new GetPendingJobsRequest(List.of(jobSource)));
    assertTrue(
        pending.getPendingJobs().stream().anyMatch(job -> jobId.equals(job.getJobId())),
        "Expected retried job to show up again as pending");
  }

  private void moveToInProgress(String jobId) throws Exception {
    pendingJobsDao.updateJobStatus(orgId, jobId, JOB_STATUS.IN_PROGRESS);
  }
}
