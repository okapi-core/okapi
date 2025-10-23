package org.okapi.logs.query;

import com.google.gson.Gson;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.okapi.logs.StaticConfiguration;
import org.okapi.logs.config.LogsCfg;
import org.okapi.logs.select.BlockMemberSelector;
import org.okapi.logs.spring.HttpClientConfiguration;
import org.okapi.protos.logs.LogPayloadProto;
import org.okapi.rest.logs.FilterNode;
import org.okapi.rest.logs.LogView;
import org.okapi.rest.logs.QueryRequest;
import org.okapi.rest.logs.QueryResponse;
import org.okapi.swim.identity.WhoAmI;
import org.okapi.swim.ping.MemberList;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class MemberSetQueryProcessor implements QueryProcessor {
  private final MemberList memberList;
  private final BlockMemberSelector selector;
  private final WhoAmI whoAmI;
  private final OkHttpClient client;
  private final Gson gson;
  private final LogsCfg logsCfg;

  public MemberSetQueryProcessor(
      MemberList memberList,
      BlockMemberSelector selector,
      WhoAmI whoAmI,
      @Qualifier(HttpClientConfiguration.LOGS_OK_HTTP) OkHttpClient client,
      LogsCfg cfg) {
    this.memberList = memberList;
    this.selector = selector;
    this.whoAmI = whoAmI;
    this.client = client;
    this.gson = new Gson();
    this.logsCfg = cfg;
  }

  @Override
  public List<LogPayloadProto> getLogs(
      String tenantId, String logStream, long start, long end, LogFilter filter, QueryConfig cfg)
      throws IOException {
    if (cfg != null && cfg.fanOut) return List.of();
    long now = System.currentTimeMillis();
    long hourStart =
        (now / this.logsCfg.getIdxExpiryDuration()) * this.logsCfg.getIdxExpiryDuration();
    long hourEnd = hourStart + this.logsCfg.getIdxExpiryDuration();
    long qStart = Math.max(start, hourStart);
    long qEnd = Math.min(end, hourEnd);
    if (qStart >= qEnd) return List.of();

    int blockIdx =
        StaticConfiguration.hashLogStream(
            tenantId, logStream, hourStart / this.logsCfg.getIdxExpiryDuration());
    var target = selector.select(tenantId, logStream, hourStart, blockIdx, memberList);
    if (target == null || target.getNodeId().equals(whoAmI.getNodeId())) return List.of();

    var req =
        QueryRequest.builder()
            .start(qStart)
            .end(qEnd)
            .limit(1000)
            .filter(toFilterNode(filter))
            .build();

    byte[] body = gson.toJson(req).getBytes();
    var url = String.format("http://%s:%d/logs/query", target.getIp(), target.getPort());
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
      QueryResponse qresp = gson.fromJson(resp.body().string(), QueryResponse.class);
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
      n.traceId = tf.getTraceId();
      return n;
    } else if (filter instanceof RegexFilter rf) {
      FilterNode n = new FilterNode();
      n.kind = "REGEX";
      n.regex = rf.getRegex();
      return n;
    } else if (filter instanceof AndFilter af) {
      FilterNode n = new FilterNode();
      n.kind = "AND";
      n.left = toFilterNode(af.getLeft());
      n.right = toFilterNode(af.getRight());
      return n;
    } else if (filter instanceof OrFilter of) {
      FilterNode n = new FilterNode();
      n.kind = "OR";
      n.left = toFilterNode(of.getLeft());
      n.right = toFilterNode(of.getRight());
      return n;
    }
    return null;
  }

  private static LogPayloadProto fromView(LogView v) {
    LogPayloadProto.Builder b = LogPayloadProto.newBuilder();
    b.setTsMillis(v.getTsMillis());
    b.setLevel(v.getLevel());
    if (v.getBody() != null) b.setBody(v.getBody());
    if (v.getTraceId() != null) b.setTraceId(v.getTraceId());
    return b.build();
  }
}
