package org.okapi.logs.query;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.okapi.logs.select.BlockMemberSelector;
import org.okapi.logs.spring.HttpClientConfiguration;
import org.okapi.rest.logs.FilterNode;
import org.okapi.rest.logs.QueryRequest;
import org.okapi.rest.logs.QueryResponse;
import org.okapi.rest.logs.LogView;
import org.okapi.protos.logs.LogPayloadProto;
import org.okapi.swim.identity.WhoAmI;
import org.okapi.swim.ping.Member;
import org.okapi.swim.ping.MemberList;
import org.springframework.stereotype.Service;

@Service
public class MemberSetQueryProcessor implements QueryProcessor {
  private final MemberList memberList;
  private final BlockMemberSelector selector;
  private final WhoAmI whoAmI;
  private final OkHttpClient client;
  private final ObjectMapper mapper;

  public MemberSetQueryProcessor(
      MemberList memberList,
      BlockMemberSelector selector,
      WhoAmI whoAmI,
      @org.springframework.beans.factory.annotation.Qualifier(HttpClientConfiguration.LOGS_OK_HTTP)
          OkHttpClient client,
      ObjectMapper mapper) {
    this.memberList = memberList;
    this.selector = selector;
    this.whoAmI = whoAmI;
    this.client = client;
    this.mapper = mapper;
  }

  @Override
  public List<LogPayloadProto> getLogs(
      String tenantId, String logStream, long start, long end, LogFilter filter, QueryConfig cfg)
      throws IOException {
    if (cfg != null && cfg.fanOut) return List.of();
    long now = System.currentTimeMillis();
    long hourStart = (now / 3600_000L) * 3600_000L;
    long hourEnd = hourStart + 3600_000L;
    long qStart = Math.max(start, hourStart);
    long qEnd = Math.min(end, hourEnd);
    if (qStart >= qEnd) return List.of();

    int blockIdx = org.okapi.logs.StaticConfiguration.hashLogStream(tenantId, logStream, hourStart / 3600_000L);
    Member target = selector.select(tenantId, logStream, hourStart, blockIdx, memberList);
    if (target == null || target.getNodeId().equals(whoAmI.getNodeId())) return List.of();

    QueryRequest req = new QueryRequest();
    req.start = qStart;
    req.end = qEnd;
    req.limit = 1000; // upper bound; pagination handled by caller if needed
    req.filter = toFilterNode(filter);

    byte[] body = mapper.writeValueAsBytes(req);
    String url = String.format("http://%s:%d/logs/query", target.getIp(), target.getPort());
    Request httpReq =
        new Request.Builder()
            .url(url)
            .post(RequestBody.create(MediaType.parse("application/json"), body))
            .addHeader("X-Okapi-Tenant-Id", tenantId)
            .addHeader("X-Okapi-Log-Stream", logStream)
            .addHeader("X-Okapi-Fan-Out", "true")
            .build();
    try (Response resp = client.newCall(httpReq).execute()) {
      if (!resp.isSuccessful() || resp.body() == null) return List.of();
      QueryResponse qresp = mapper.readValue(resp.body().bytes(), QueryResponse.class);
      List<LogPayloadProto> out = new ArrayList<>();
      if (qresp.items != null) {
        for (LogView v : qresp.items) out.add(fromView(v));
      }
      return out;
    }
  }

  private static FilterNode toFilterNode(LogFilter filter) {
    if (filter == null) return null;
    if (filter instanceof LevelFilter lf) {
      FilterNode n = new FilterNode();
      n.kind = "LEVEL";
      n.levelCode = lf.levelCode();
      return n;
    } else if (filter instanceof TraceFilter tf) {
      FilterNode n = new FilterNode();
      n.kind = "TRACE";
      n.traceId = tf.traceId();
      return n;
    } else if (filter instanceof RegexFilter rf) {
      FilterNode n = new FilterNode();
      n.kind = "REGEX";
      n.regex = rf.regex();
      return n;
    } else if (filter instanceof AndFilter af) {
      FilterNode n = new FilterNode();
      n.kind = "AND";
      n.left = toFilterNode(af.left());
      n.right = toFilterNode(af.right());
      return n;
    } else if (filter instanceof OrFilter of) {
      FilterNode n = new FilterNode();
      n.kind = "OR";
      n.left = toFilterNode(of.left());
      n.right = toFilterNode(of.right());
      return n;
    }
    return null;
  }

  private static LogPayloadProto fromView(LogView v) {
    LogPayloadProto.Builder b = LogPayloadProto.newBuilder();
    if (v.tsMillis != null) b.setTsMillis(v.tsMillis);
    if (v.level != null) b.setLevel(v.level);
    if (v.body != null) b.setBody(v.body);
    if (v.traceId != null) b.setTraceId(v.traceId);
    return b.build();
  }
}
