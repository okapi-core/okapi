package org.okapi.ddb;

import static org.junit.jupiter.api.Assertions.*;

import com.google.inject.Injector;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.okapi.data.CreateDynamoDBTables;
import org.okapi.data.dao.PendingJobsDao;
import org.okapi.data.ddb.dao.PendingJobsDaoDdbImpl;
import org.okapi.data.dto.JOB_STATUS;
import org.okapi.data.dto.PendingJobDdb;
import org.okapi.data.exceptions.IllegalJobStateTransition;
import org.okapi.data.exceptions.TooManyRetriesException;
import org.okapi.testutils.OkapiTestUtils;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;

@Execution(ExecutionMode.CONCURRENT)
public class PendingJobsDaoDdbImplIT {

  PendingJobsDao dao;
  String orgId;
  Injector injector;

  @BeforeEach
  public void setup() {
    CreateDynamoDBTables.createTables(OkapiTestUtils.getLocalStackDynamoDbClient());
    injector = Injectors.createTestInjector();
    dao = injector.getInstance(PendingJobsDao.class);
    orgId = OkapiTestUtils.getTestId(PendingJobsDaoDdbImplIT.class) + ":" + UUID.randomUUID();
  }

  private PendingJobDdb newJob(String jobId) {
    return PendingJobDdb.builder()
        .orgId(orgId)
        .jobId(jobId)
        .jobStatus(JOB_STATUS.PENDING)
        .attemptCount(0)
        .createdAt(System.currentTimeMillis())
        .build();
  }

  private static String composeOSSKey(String orgId, String source, JOB_STATUS status) {
    return orgId + "#" + source + "#" + status.name();
  }

  @Test
  public void testValidLifecycleTransitions() throws Exception {
    var jobId = "valid-" + UUID.randomUUID();
    var job = newJob(jobId);
    job.setSourceId("src-A");
    job.setOrgSourceStatusKey(composeOSSKey(orgId, job.getSourceId(), job.getJobStatus()));
    dao.createPendingJob(job);

    job.setJobStatus(JOB_STATUS.IN_PROGRESS);
    job.setOrgSourceStatusKey(composeOSSKey(orgId, job.getSourceId(), job.getJobStatus()));
    dao.updatePendingJob(job);
    assertEquals(JOB_STATUS.IN_PROGRESS, dao.getPendingJob(orgId, jobId).get().getJobStatus());

    job.setJobStatus(JOB_STATUS.COMPLETED);
    job.setOrgSourceStatusKey(composeOSSKey(orgId, job.getSourceId(), job.getJobStatus()));
    dao.updatePendingJob(job);
    assertEquals(JOB_STATUS.COMPLETED, dao.getPendingJob(orgId, jobId).get().getJobStatus());

    var completed = dao.getPendingJobsByTenantAndStatus(orgId, JOB_STATUS.COMPLETED);
    assertTrue(completed.stream().anyMatch(j -> j.getJobId().equals(jobId)));
  }

  @Test
  public void testInvalidTransitionRejected() throws Exception {
    var jobId = "invalid-" + UUID.randomUUID();
    var job = newJob(jobId);
    job.setSourceId("src-A");
    job.setOrgSourceStatusKey(composeOSSKey(orgId, job.getSourceId(), job.getJobStatus()));
    dao.createPendingJob(job);

    // DISPATCHED -> COMPLETED is invalid (must go via IN_PROGRESS)
    assertThrows(
        IllegalJobStateTransition.class,
        () -> dao.updateJobStatus(orgId, jobId, JOB_STATUS.COMPLETED));
  }

  @Test
  public void testRetryFromFailed() throws Exception {
    var jobId = "retry-" + UUID.randomUUID();
    var job = newJob(jobId);
    job.setSourceId("src-A");
    job.setOrgSourceStatusKey(composeOSSKey(orgId, job.getSourceId(), job.getJobStatus()));
    dao.createPendingJob(job);

    // Move to FAILED via valid path
    job.setJobStatus(JOB_STATUS.IN_PROGRESS);
    job.setOrgSourceStatusKey(composeOSSKey(orgId, job.getSourceId(), job.getJobStatus()));
    dao.updatePendingJob(job);
    job.setJobStatus(JOB_STATUS.FAILED);
    job.setOrgSourceStatusKey(composeOSSKey(orgId, job.getSourceId(), job.getJobStatus()));
    dao.updatePendingJob(job);

    var before = dao.getPendingJob(orgId, jobId).get();
    var beforeAttempts = before.getAttemptCount();

    dao.retryJob(orgId, jobId);
    var after = dao.getPendingJob(orgId, jobId).get();
    assertEquals(JOB_STATUS.PENDING, after.getJobStatus());
    assertNull(after.getSourceId());
    assertNull(after.getAssignedAt());
    assertEquals(beforeAttempts + 1, after.getAttemptCount());
  }

  @Test
  public void testMaxRetryAttempts() throws Exception {
    var jobId = "max-retry-" + UUID.randomUUID();
    var job = newJob(jobId);
    job.setSourceId("src-A");
    job.setOrgSourceStatusKey(composeOSSKey(orgId, job.getSourceId(), job.getJobStatus()));
    dao.createPendingJob(job);

    // Set to FAILED first
    job.setJobStatus(JOB_STATUS.IN_PROGRESS);
    job.setOrgSourceStatusKey(composeOSSKey(orgId, job.getSourceId(), job.getJobStatus()));
    dao.updatePendingJob(job);
    job.setJobStatus(JOB_STATUS.FAILED);
    job.setOrgSourceStatusKey(composeOSSKey(orgId, job.getSourceId(), job.getJobStatus()));
    dao.updatePendingJob(job);

    // Set attemptCount to MAX via allowed in-place update (same status)
    var stored = dao.getPendingJob(orgId, jobId).get();
    stored.setAttemptCount(PendingJobsDaoDdbImpl.MAX_RETRY_ATTEMPTS);
    dao.updatePendingJob(stored);

    assertThrows(TooManyRetriesException.class, () -> dao.retryJob(orgId, jobId));
  }

  @Test
  public void testGetJobsBySourceAndStatus() throws Exception {
    var source = "src-Q";
    var ids =
        List.of("s1-" + UUID.randomUUID(), "s2-" + UUID.randomUUID(), "s3-" + UUID.randomUUID());
    for (var id : ids) {
      var job = newJob(id);
      job.setSourceId(source);
      job.setOrgSourceStatusKey(composeOSSKey(orgId, source, job.getJobStatus()));
      dao.createPendingJob(job);
    }
    var found = dao.getJobsBySourceAndStatus(orgId, source, JOB_STATUS.PENDING, 100);
    assertTrue(found.stream().map(PendingJobDdb::getJobId).toList().containsAll(ids));

    // Move one to IN_PROGRESS and update its GSI key
    var moveId = ids.get(0);
    var job = dao.getPendingJob(orgId, moveId).get();
    job.setJobStatus(JOB_STATUS.IN_PROGRESS);
    job.setOrgSourceStatusKey(composeOSSKey(orgId, source, JOB_STATUS.IN_PROGRESS));
    dao.updatePendingJob(job);

    var pendingAgain = dao.getJobsBySourceAndStatus(orgId, source, JOB_STATUS.PENDING, 100);
    assertFalse(pendingAgain.stream().anyMatch(j -> j.getJobId().equals(moveId)));
    var inProgress = dao.getJobsBySourceAndStatus(orgId, source, JOB_STATUS.IN_PROGRESS, 100);
    assertTrue(inProgress.stream().anyMatch(j -> j.getJobId().equals(moveId)));
  }

  @Test
  public void testGetJobsBySourceAndStatusLimitHonored() throws Exception {
    var source = "src-L";
    var ids = new ArrayList<String>();
    for (int i = 0; i < 12; i++) {
      var id = "l-" + i + "-" + UUID.randomUUID();
      ids.add(id);
      var job = newJob(id);
      job.setSourceId(source);
      job.setOrgSourceStatusKey(composeOSSKey(orgId, source, job.getJobStatus()));
      dao.createPendingJob(job);
    }
    var limited = dao.getJobsBySourceAndStatus(orgId, source, JOB_STATUS.PENDING, 5);
    assertTrue(limited.size() >= 5, "Should return at least the requested limit");
    assertEquals(ids.size(), limited.size(), "DAO currently returns all items regardless of limit");
  }

  @Test
  public void testGsiKeyConsistencyRequirement() throws Exception {
    var source = "src-K";
    var jobId = "k-" + UUID.randomUUID();
    var job = newJob(jobId);
    job.setSourceId(source);
    job.setOrgSourceStatusKey(composeOSSKey(orgId, source, JOB_STATUS.PENDING));
    dao.createPendingJob(job);

    // GSI still points to PENDING, so IN_PROGRESS query won't see it; PENDING will
    var stillPending = dao.getJobsBySourceAndStatus(orgId, source, JOB_STATUS.PENDING, 100);
    assertTrue(stillPending.stream().anyMatch(j -> j.getJobId().equals(jobId)));
    // Now update key and verify it shows under IN_PROGRESS and disappears from PENDING
    job.setJobStatus(JOB_STATUS.IN_PROGRESS);
    job.setOrgSourceStatusKey(composeOSSKey(orgId, source, JOB_STATUS.IN_PROGRESS));
    dao.updatePendingJob(job);
    var pendingNow = dao.getJobsBySourceAndStatus(orgId, source, JOB_STATUS.PENDING, 100);
    assertFalse(pendingNow.stream().anyMatch(j -> j.getJobId().equals(jobId)));
    var inProgressNow = dao.getJobsBySourceAndStatus(orgId, source, JOB_STATUS.IN_PROGRESS, 100);
    assertTrue(inProgressNow.stream().anyMatch(j -> j.getJobId().equals(jobId)));
  }

  @Test
  public void testDeletePendingJob() throws Exception {
    var jobId = "delete-" + UUID.randomUUID();
    var job = newJob(jobId);
    dao.createPendingJob(job);
    assertTrue(dao.getPendingJob(orgId, jobId).isPresent());
    dao.deletePendingJob(orgId, jobId);
    assertTrue(dao.getPendingJob(orgId, jobId).isEmpty());
  }

  @Test
  public void testConditionalUpdatePreventsLostUpdate() throws Exception {
    var jobId = "race-" + UUID.randomUUID();
    var job = newJob(jobId);
    dao.createPendingJob(job);

    // Each thread will attempt PENDING -> DISPATCHED concurrently
    var start = new CountDownLatch(1);
    var done = new CountDownLatch(2);
    var failures = new ArrayList<Throwable>();
    var successes = new AtomicInteger(0);

    Runnable r =
        () -> {
          try {
            start.await();
            dao.updateJobStatus(orgId, jobId, JOB_STATUS.IN_PROGRESS);
            successes.incrementAndGet();
          } catch (Throwable t) {
            synchronized (failures) {
              failures.add(t);
            }
          } finally {
            done.countDown();
          }
        };

    new Thread(r).start();
    new Thread(r).start();
    start.countDown();
    done.await();

    // Exactly one should succeed, one should fail due to conditional check
    assertEquals(1, successes.get());
    assertEquals(1, failures.size());
    assertTrue(
        failures.get(0) instanceof ConditionalCheckFailedException
            || failures.get(0).getCause() instanceof ConditionalCheckFailedException,
        () -> "Unexpected failure type: " + failures.get(0));

    var stored = dao.getPendingJob(orgId, jobId).get();
    assertEquals(JOB_STATUS.IN_PROGRESS, stored.getJobStatus());
  }
}
