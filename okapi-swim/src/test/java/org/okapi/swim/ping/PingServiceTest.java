package org.okapi.swim.ping;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.Gson;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.okapi.swim.config.SwimConfig;
import org.okapi.swim.disseminate.Disseminator;
import org.okapi.swim.membership.MembershipService;
import org.okapi.swim.rest.AckMessage;

class PingServiceTest {

  MockWebServer server;

  @BeforeEach
  void setup() throws IOException {
    server = new MockWebServer();
    server.start();
  }

  @AfterEach
  void tearDown() throws IOException {
    server.shutdown();
  }

  private PingService build(MemberList list) {
    SwimConfig cfg = new SwimConfig();
    cfg.setK(1);
    cfg.setTimeoutMillis(2000);
    cfg.setRetries(0);
    ExecutorService ex = Executors.newFixedThreadPool(2);
    MembershipService membershipService = Mockito.mock(MembershipService.class);
    Disseminator disseminator = Mockito.mock(Disseminator.class);
    return new PingService(
        list,
        cfg,
        new Gson(),
        new OkHttpClient(),
        ex,
        membershipService,
        disseminator);
  }

  @Test
  void ping_success() {
    server.enqueue(new MockResponse().setResponseCode(200).setBody("{}"));
    MemberList list = new MemberList();
    list.addMember(new Member("n1", "localhost", server.getPort()));

    var svc = build(list);
    var result = svc.ping("n1");
    assertNull(result.getException());
    assertNotNull(result.getResult());
  }

  @Test
  void ping_httpError() {
    server.enqueue(new MockResponse().setResponseCode(500));
    MemberList list = new MemberList();
    list.addMember(new Member("n1", "localhost", server.getPort()));

    var svc = build(list);
    var result = svc.ping("n1");
    assertNull(result.getResult());
    assertNotNull(result.getException());
  }

  @Test
  void pingKIndirect_oneSuccess() throws Exception {
    server.enqueue(new MockResponse().setResponseCode(200).setBody("{}"));
    MemberList list = new MemberList();
    list.addMember(new Member("n1", "localhost", server.getPort()));

    var svc = build(list);
    Optional<AckMessage> ack = svc.pingKIndirect("n2");
    assertTrue(ack.isPresent());
  }
}
