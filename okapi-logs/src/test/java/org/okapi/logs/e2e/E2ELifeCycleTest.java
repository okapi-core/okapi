package org.okapi.logs.e2e;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.okapi.logs.TestApplication;
import org.okapi.logs.config.LogsCfgImpl;
import org.okapi.logs.spring.AwsConfiguration;
import org.okapi.logs.testutil.OtelTestPayloads;
import org.okapi.rest.logs.FilterNode;
import org.okapi.rest.logs.LogView;
import org.okapi.rest.logs.QueryRequest;
import org.okapi.rest.logs.QueryResponse;
import org.okapi.swim.membership.MembershipEventPublisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;

@Slf4j
@SpringBootTest(
    classes = {TestApplication.class, AwsConfiguration.class},
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestPropertySource(
    properties = {
      // Keep pages tiny so persister flushes quickly
      "okapi.logs.maxDocsPerPage=2",
      "okapi.logs.maxPageBytes=65536",
      "okapi.logs.maxPageWindowMs=100",
      "okapi.logs.s3.endpoint=http://localhost:4566",
      // S3 settings wired for Localstack via AwsConfiguration (test profile)
      "okapi.logs.s3.basePrefix=e2e-logs",
      // Fast upload ticks and short time partitioning for test
      "okapi.logs.s3.uploadIntervalMs=200",
      "okapi.logs.idxExpiryDuration=1000",
      "okapi.logs.s3.uploadGraceMs=0",
      // Satisfy region bean
      "okapi.logs.s3.region=us-east-1"
    })
class E2ELifeCycleTest {

  private static Path testDataDir;

  @DynamicPropertySource
  static void dynamicProps(DynamicPropertyRegistry registry) throws Exception {
    testDataDir = Files.createTempDirectory("okapi-logs-e2e-");
    registry.add("okapi.logs.dataDir", () -> testDataDir.toString());
  }

  @LocalServerPort private int port;

  @Autowired private TestRestTemplate restTemplate;
  @Autowired private S3Client s3;
  @Autowired private LogsCfgImpl cfg;
  @MockitoBean MembershipEventPublisher membershipEventPublisher;

  private String baseUrl;

  @BeforeEach
  void setUp() {
    baseUrl = "http://localhost:" + port;
  }

  @Test
  void ingest_query_wait_upload_query_again() {
    String tenant = UUID.randomUUID().toString();
    String stream = "stream-e2e";

    // Build corpus safely in the past so it's not the current partition hour
    long idxDur = cfg.getIdxExpiryDuration();
    long baseTsMillis = System.currentTimeMillis() - (2 * idxDur);
    OtelTestPayloads.Result payload = OtelTestPayloads.checkoutScenario(baseTsMillis);

    // Ingest via OTLP/HTTP
    HttpHeaders ingestHeaders = new HttpHeaders();
    ingestHeaders.setContentType(MediaType.APPLICATION_OCTET_STREAM);
    ingestHeaders.add("X-Okapi-Tenant-Id", tenant);
    ingestHeaders.add("X-Okapi-Log-Stream", stream);
    ResponseEntity<String> ingestResp =
        restTemplate.postForEntity(
            baseUrl + "/v1/logs", new HttpEntity<>(payload.body(), ingestHeaders), String.class);
    assertEquals(HttpStatus.OK, ingestResp.getStatusCode());

    long start = payload.startTsMillis() - 60_000;
    long end = payload.startTsMillis() + 60_000;

    // Pre-upload queries (buffer+disk) — wait until counts stabilize
    Awaitility.await()
        .atMost(Duration.ofSeconds(10))
        .untilAsserted(
            () -> {
              var traceACount =
                  queryCount(
                      tenant, stream, start, end, traceFilter(OtelTestPayloads.TRACE_A_HEX), null);
              log.info("Got traceACount {}", traceACount);
              assertEquals(5, traceACount);
              var failedFilterCount =
                  queryCount(tenant, stream, start, end, regexFilter("failed"), null);
              log.info("Got failedFilterCount {}", failedFilterCount);
              assertEquals(2, failedFilterCount);
            });
    // Wait at least one idxExpiryDuration to make prior hour eligible (even though base is older)
    try {
      Thread.sleep(idxDur + 300);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    // Await S3 upload for that hour (any node path under the hour prefix)
    long hr = payload.startTsMillis() / cfg.getIdxExpiryDuration();
    String hourPrefix = cfg.getS3BasePrefix() + "/" + tenant + "/" + stream + "/" + hr + "/";
    log.info("Checking prefix : " + hourPrefix);
    Awaitility.await()
        .atMost(Duration.ofSeconds(20))
        .untilAsserted(
            () -> {
              log.info("Checking prefix {}", hourPrefix);
              var list =
                  s3.listObjectsV2(
                      ListObjectsV2Request.builder()
                          .bucket(Objects.requireNonNull(cfg.getS3Bucket()))
                          .prefix(hourPrefix)
                          .build());
              assertTrue(
                  list.hasContents()
                      && list.contents().stream()
                          .map(o -> o.key())
                          .anyMatch(k -> k.endsWith("/logfile.idx")),
                  "Expected index in S3 under prefix: " + hourPrefix);
            });

    // Post-upload queries (S3 + disk). We expect the same counts (no duplication).
    int traceCount =
        queryCount(tenant, stream, start, end, traceFilter(OtelTestPayloads.TRACE_A_HEX), null);
    int failedCount = queryCount(tenant, stream, start, end, regexFilter("failed"), null);
    assertEquals(5, traceCount);
    assertEquals(2, failedCount);
  }

  private int queryCount(
      String tenant, String stream, long start, long end, FilterNode filter, String fanOutHeader) {
    QueryRequest req =
        QueryRequest.builder()
            .start(start)
            .end(end)
            .limit(1000)
            .filter(filter)
            .pageToken(null)
            .build();
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.add("X-Okapi-Tenant-Id", tenant);
    headers.add("X-Okapi-Log-Stream", stream);
    if (fanOutHeader != null) headers.add("X-Okapi-Fan-Out", fanOutHeader);
    ResponseEntity<QueryResponse> resp =
        restTemplate.postForEntity(
            baseUrl + "/logs/query", new HttpEntity<>(req, headers), QueryResponse.class);
    if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) return 0;
    List<LogView> items = resp.getBody().items;
    return items == null ? 0 : items.size();
  }

  private static FilterNode regexFilter(String regex) {
    FilterNode n = new FilterNode();
    n.kind = "REGEX";
    n.regex = regex;
    return n;
  }

  private static FilterNode traceFilter(String traceId) {
    FilterNode n = new FilterNode();
    n.kind = "TRACE";
    n.traceId = traceId;
    return n;
  }
}
