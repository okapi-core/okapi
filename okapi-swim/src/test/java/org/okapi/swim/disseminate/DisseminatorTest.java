package org.okapi.swim.disseminate;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.Gson;
import java.io.IOException;
import java.util.concurrent.Executors;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.okapi.swim.TestWhoAmiFactory;
import org.okapi.swim.config.SwimConfig;
import org.okapi.swim.identity.WhoAmI;
import org.okapi.swim.ping.Member;
import org.okapi.swim.ping.MemberList;

class DisseminatorTest {

  MockWebServer serverA;
  MockWebServer serverB;

  @BeforeEach
  void setup() throws IOException {
    serverA = new MockWebServer();
    serverB = new MockWebServer();
    serverA.start();
    serverB.start();
  }

  @AfterEach
  void tearDown() throws IOException {
    serverA.shutdown();
    serverB.shutdown();
  }

  @Test
  void disseminate_successToAllPeers() {
    serverA.enqueue(new MockResponse().setResponseCode(200).setBody("{}"));
    serverB.enqueue(new MockResponse().setResponseCode(200).setBody("{}"));

    var memberList = new MemberList();
    memberList.addMember(new Member("node-a", "localhost", serverA.getPort()));
    memberList.addMember(new Member("node-b", "localhost", serverB.getPort()));
    // also add self to ensure it's skipped
    memberList.addMember(new Member("self-node", "localhost", 65535));

    OkHttpClient client = new OkHttpClient();
    WhoAmI whoAmI = TestWhoAmiFactory.makeWhoAmI("self-node", "localhost", 8080);
    var disseminator =
        new Disseminator(
            memberList,
            client,
            whoAmI,
            Executors.newFixedThreadPool(2),
            new Gson(),
            new SwimConfig());

    var result = disseminator.disseminateUnHealthy("node-x");
    assertNull(result.getException());
    assertNotNull(result.getResult());
  }

  @Test
  void disseminate_firstFailureReturnsError() {
    serverA.enqueue(new MockResponse().setResponseCode(500));
    serverB.enqueue(new MockResponse().setResponseCode(200).setBody("{}"));

    var memberList = new MemberList();
    memberList.addMember(new Member("node-a", "localhost", serverA.getPort()));
    memberList.addMember(new Member("node-b", "localhost", serverB.getPort()));

    OkHttpClient client = new OkHttpClient();
    WhoAmI whoAmI = TestWhoAmiFactory.makeWhoAmI("self-node", "localhost", 8080);
    var disseminator =
        new Disseminator(
            memberList,
            client,
            whoAmI,
            Executors.newFixedThreadPool(2),
            new Gson(),
            new SwimConfig());
    var result = disseminator.disseminateUnHealthy("node-x");
    assertNull(result.getResult());
    assertNotNull(result.getException());
  }
}
