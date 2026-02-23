package org.okapi.web.agent;

import static org.junit.jupiter.api.Assertions.*;
import static org.okapi.web.auth.TestCommons.addToOrg;

import com.google.inject.Guice;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.okapi.agent.dto.PendingJob;
import org.okapi.agent.dto.QueryResult;
import org.okapi.agent.dto.QuerySpec;
import org.okapi.data.dao.PendingJobsDao;
import org.okapi.data.dao.RelationGraphDao;
import org.okapi.data.dao.UsersDao;
import org.okapi.okapi_agent.CmdParser;
import org.okapi.okapi_agent.RuntimeConfigModule;
import org.okapi.okapi_agent.jobhandler.JobHandlerModule;
import org.okapi.okapi_agent.jobhandler.PendingJobRunner;
import org.okapi.web.auth.AbstractIT;
import org.okapi.web.auth.ApiTokenManager;
import org.okapi.web.auth.OrgManager;
import org.okapi.web.auth.UserManager;
import org.okapi.web.dtos.auth.CreateUserRequest;
import org.okapi.web.dtos.auth.SignInRequest;
import org.okapi.web.service.federation.dispatcher.HttpDispatcherImpl;
import org.okapi.web.service.token.Permissions;
import org.okapi.web.spring.config.S3Cfg;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import software.amazon.awssdk.services.s3.S3Client;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("test")
public class AgentMainIT extends AbstractIT {

  @Autowired private ApiTokenManager apiTokenManager;
  @Autowired private PendingJobsDao pendingJobsDao;
  @Autowired private UserManager userManager;
  @Autowired private OrgManager orgManager;
  @Autowired private UsersDao usersDao;
  @Autowired private RelationGraphDao relationGraphDao;
  @Autowired private S3Client s3Client;
  @Autowired private S3Cfg s3Cfg;
  @Autowired HttpDispatcherImpl dispatcher;

  private MockWebServer mockWebServer;
  private ScheduledExecutorService scheduler;
  private PendingJobRunner agentRunner;
  private Thread agentThread;
  private String orgId;
  private String sourceId;
  private Path tokenFile;
  private Path handlerCfgFile;

  @BeforeEach
  void setupEach() throws Exception {
    super.setup();
    mockWebServer = new MockWebServer();
    mockWebServer.start();
    scheduler = Executors.newScheduledThreadPool(1);

    var email = dedup("agent-main@test.com", this.getClass());
    var password = "pw";
    try {
      userManager.signupWithEmailPassword(new CreateUserRequest("Agent", "User", email, password));
    } catch (Exception ignored) {
    }
    var loginToken = userManager.signInWithEmailPassword(new SignInRequest(email, password));
    orgId = orgManager.listOrgs(loginToken).getOrgs().get(0).getOrgId();
    addToOrg(usersDao, relationGraphDao, orgId, email, true);

    var apiToken =
        apiTokenManager.createApiToken(
            orgId, List.of(Permissions.AGENT_JOBS_READ, Permissions.AGENT_JOBS_UPDATE));

    sourceId = "agent-source-" + UUID.randomUUID();
    tokenFile = Files.createTempFile("gateway-token", ".txt");
    Files.writeString(tokenFile, apiToken);

    handlerCfgFile = Files.createTempFile("handler-cfg", ".yaml");
    var handlerCfg =
        """
        - type: http
          sourceId: %s
          host: "%s"
          headers: {}
        """
            .formatted(sourceId, mockWebServer.url("/").toString());
    Files.writeString(handlerCfgFile, handlerCfg);
    var portGenerator = new SecureRandom();
    var port = portGenerator.nextInt(9090, 10000);
    startAgent(tokenFile, handlerCfgFile, port);
  }

  @AfterEach
  void teardown() throws Exception {
    if (agentRunner != null) {
      agentRunner.stop();
    }
    if (agentThread != null) {
      agentThread.interrupt();
    }
    if (scheduler != null) {
      scheduler.shutdownNow();
    }
    if (mockWebServer != null) {
      mockWebServer.close();
    }
    if (tokenFile != null) {
      Files.deleteIfExists(tokenFile);
    }
    if (handlerCfgFile != null) {
      Files.deleteIfExists(handlerCfgFile);
    }
  }

  @Test
  void agent_handles_http_job_successfully() throws Exception {
    mockWebServer.enqueue(
        new MockResponse.Builder()
            .code(200)
            .body("{\"status\":\"success\"}")
            .addHeader("Content-Type", "application/json")
            .build());

    var pendingJob =
        PendingJob.builder()
            .jobId(UUID.randomUUID().toString())
            .sourceId(sourceId)
            .spec(
                QuerySpec.builder()
                    .serializedQuery("GET /prom/api/v1/query HTTP/1.1\r\nHost: example.com\r\n\r\n")
                    .build())
            .build();

    CompletableFuture<QueryResult> future = dispatcher.dispatchJob(orgId, pendingJob);

    var response = future.get(5, TimeUnit.SECONDS);
    assertNotNull(response);
    assertNull(response.error(), "Expected successful job result");
    assertNotNull(response.data());
    assertTrue(response.data().contains("success"));
    var json = com.google.gson.JsonParser.parseString(response.data()).getAsJsonObject();
    assertEquals("success", json.get("status").getAsString());

    var recorded = mockWebServer.takeRequest(2, TimeUnit.SECONDS);
    assertNotNull(recorded, "Expected a request to be dispatched to the mock server");
    assertEquals("/prom/api/v1/query", recorded.getTarget());
    assertEquals("GET", recorded.getMethod());
    // Headers: the handler merges config headers (none) with the request's headers from the
    // serialized query
    assertEquals("example.com", recorded.getHeaders().get("Host"));
  }

  @Test
  void agent_handles_http_post_job_successfully() throws Exception {
    mockWebServer.enqueue(
        new MockResponse.Builder()
            .code(200)
            .body("{\"status\":\"post-success\"}")
            .addHeader("Content-Type", "application/json")
            .build());

    String body = "{\"query\":\"up\"}";
    var pendingJob =
        PendingJob.builder()
            .jobId(UUID.randomUUID().toString())
            .sourceId(sourceId)
            .spec(
                QuerySpec.builder()
                    .serializedQuery(
                        "POST /prom/api/v1/query HTTP/1.1\r\n"
                            + "Host: example.com\r\n"
                            + "X-Okapi-Gateway-Id: Gateway-Id\r\n"
                            + "Content-Type: application/json\r\n"
                            + "\r\n"
                            + body)
                    .build())
            .build();

    CompletableFuture<QueryResult> future = dispatcher.dispatchJob(orgId, pendingJob);

    var response = future.get(5, TimeUnit.SECONDS);
    assertNotNull(response);
    assertNull(response.error(), "Expected successful job result");
    assertNotNull(response.data());
    var json = com.google.gson.JsonParser.parseString(response.data()).getAsJsonObject();
    assertEquals("post-success", json.get("status").getAsString());

    var recorded = mockWebServer.takeRequest(2, TimeUnit.SECONDS);
    assertNotNull(recorded, "Expected a request to be dispatched to the mock server");
    assertEquals("/prom/api/v1/query", recorded.getTarget());
    assertEquals("POST", recorded.getMethod());
    assertEquals("example.com", recorded.getHeaders().get("Host"));
    assertEquals("application/json", recorded.getHeaders().get("Content-Type"));
    assertEquals("Gateway-Id", recorded.getHeaders().get("X-Okapi-Gateway-Id"));
    assertEquals(body, recorded.getBody().string(StandardCharsets.UTF_8));
  }

  private void startAgent(Path tokenPath, Path handlerPath, int port) {
    agentThread =
        new Thread(
            () -> {
              var args =
                  new String[] {
                    "--gateway-endpoint",
                    "http://localhost:" + port,
                    "--gateway-token-path",
                    tokenPath.toString(),
                    "--sources-config-path",
                    handlerPath.toString(),
                    "--poll-delay",
                    "200"
                  };
              var staticConfig = CmdParser.fromArgs(args);
              var injector =
                  Guice.createInjector(
                      new JobHandlerModule(), new RuntimeConfigModule(staticConfig));
              agentRunner = injector.getInstance(PendingJobRunner.class);
              agentRunner.start();
            });
    agentThread.setDaemon(true);
    agentThread.start();
    try {
      Thread.sleep(Duration.ofMillis(300));
    } catch (InterruptedException ignored) {
    }
  }
}
