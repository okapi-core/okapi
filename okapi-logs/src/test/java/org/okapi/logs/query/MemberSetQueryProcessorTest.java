package org.okapi.logs.query;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.okapi.logs.config.LogsCfg;
import org.okapi.logs.config.LogsCfgImpl;
import org.okapi.logs.select.BlockMemberSelector;
import org.okapi.protos.logs.LogPayloadProto;
import org.okapi.rest.logs.LogView;
import org.okapi.rest.logs.QueryResponse;
import org.okapi.swim.identity.WhoAmI;
import org.okapi.swim.ping.Member;
import org.okapi.swim.ping.MemberList;

class MemberSetQueryProcessorTest {
  private MockWebServer server;
  private LogsCfg logsCfg;

  @BeforeEach
  void start() throws Exception {
    logsCfg = new LogsCfgImpl("test-bucket");
    server = new MockWebServer();
    server.start();
  }

  @AfterEach
  void stop() throws Exception {
    server.shutdown();
  }

  @Test
  void returnsEmpty_whenFanOutTrue_orSelfTarget_orOutsideHour() throws Exception {
    var list = new MemberList();
    var self =
        new WhoAmI() {
          @Override
          public String getNodeId() {
            return "self";
          }

          @Override
          public String getNodeIp() {
            return "127.0.0.1";
          }

          @Override
          public int getNodePort() {
            return 8080;
          }
        };
    list.addMember(new Member("self", self.getNodeIp(), self.getNodePort()));
    BlockMemberSelector selector = mock(BlockMemberSelector.class);
    when(selector.select(any(), any(), anyLong(), anyInt(), any()))
        .thenReturn(new Member("self", self.getNodeIp(), self.getNodePort()));

    MemberSetQueryProcessor qp =
        new MemberSetQueryProcessor(list, selector, self, new OkHttpClient(), logsCfg);

    long now = System.currentTimeMillis();
    long hourStart = (now / logsCfg.getIdxExpiryDuration()) * logsCfg.getIdxExpiryDuration();

    // outside hour
    assertTrue(
        qp.getLogs(
                "t",
                "s",
                hourStart - logsCfg.getIdxExpiryDuration(),
                hourStart - 1,
                new RegexFilter(".*"),
                QueryConfig.defaultConfig())
            .isEmpty());
    // fanOut
    assertTrue(
        qp.getLogs(
                "t",
                "s",
                hourStart,
                hourStart + 1000,
                new RegexFilter(".*"),
                new QueryConfig(true, true, true, true))
            .isEmpty());
    // self target
    assertTrue(
        qp.getLogs(
                "t",
                "s",
                hourStart,
                hourStart + 1000,
                new RegexFilter(".*"),
                QueryConfig.defaultConfig())
            .isEmpty());
  }

  @Test
  void callsRemote_andReturnsConvertedItems() throws Exception {
    MemberList list = new MemberList();
    WhoAmI self =
        new WhoAmI() {
          @Override
          public String getNodeId() {
            return "self";
          }

          @Override
          public String getNodeIp() {
            return "127.0.0.1";
          }

          @Override
          public int getNodePort() {
            return 8080;
          }
        };
    list.addMember(new Member("self", self.getNodeIp(), self.getNodePort()));
    Member remote = new Member("remote", server.getHostName(), server.getPort());
    list.addMember(remote);

    BlockMemberSelector selector = mock(BlockMemberSelector.class);
    when(selector.select(any(), any(), anyLong(), anyInt(), any())).thenReturn(remote);

    var mapper = new ObjectMapper();
    var qp = new MemberSetQueryProcessor(list, selector, self, new OkHttpClient(), logsCfg);

    // Prepare response
    QueryResponse resp = new QueryResponse();
    LogView v1 = new LogView();
    v1.tsMillis = 1L;
    v1.level = 20;
    v1.body = "a";
    v1.traceId = "x";

    LogView v2 = new LogView();
    v2.tsMillis = 2L;
    v2.level = 30;
    v2.body = "b";
    v2.traceId = "y";
    resp.items = java.util.List.of(v1, v2);
    byte[] body = mapper.writeValueAsBytes(resp);
    server.enqueue(
        new MockResponse()
            .setResponseCode(200)
            .setBody(new String(body))
            .addHeader("Content-Type", "application/json"));

    long now = System.currentTimeMillis();
    long hourStart = (now / logsCfg.getIdxExpiryDuration()) * logsCfg.getIdxExpiryDuration();
    List<LogPayloadProto> out =
        qp.getLogs(
            "t",
            "s",
            hourStart - 1000,
            hourStart + logsCfg.getIdxExpiryDuration() + 1000,
            new RegexFilter(".*"),
            QueryConfig.defaultConfig());

    assertEquals(2, out.size());
    assertEquals("a", out.get(0).getBody());
    assertEquals(20, out.get(0).getLevel());
  }
}
