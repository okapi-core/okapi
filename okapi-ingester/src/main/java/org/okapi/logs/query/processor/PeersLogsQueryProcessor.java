package org.okapi.logs.query.processor;

import com.google.gson.Gson;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.okapi.abstractfilter.AndPageFilter;
import org.okapi.abstractfilter.OrPageFilter;
import org.okapi.abstractfilter.PageFilter;
import org.okapi.abstractio.LogStreamIdentifier;
import org.okapi.header.Headers;
import org.okapi.identity.MemberList;
import org.okapi.identity.WhoAmI;
import org.okapi.logs.config.LogsCfg;
import org.okapi.logs.io.LogPageMetadata;
import org.okapi.logs.query.LevelPageFilter;
import org.okapi.logs.query.LogPageTraceFilter;
import org.okapi.logs.query.QueryConfig;
import org.okapi.logs.query.RegexPageFilter;
import org.okapi.primitives.BinaryLogRecordV1;
import org.okapi.queryproc.AbstractPeerQueryProcessor;
import org.okapi.queryproc.FanoutGrouper;
import org.okapi.queryproc.LogsQueryProcessor;
import org.okapi.rest.logs.FilterNode;
import org.okapi.rest.logs.LogView;
import org.okapi.rest.logs.QueryRequest;
import org.okapi.rest.logs.QueryResponse;
import org.okapi.spring.configs.HttpClientConfiguration;
import org.okapi.spring.configs.properties.QueryCfg;
import org.okapi.streams.StreamIdentifier;
import org.springframework.beans.factory.annotation.Qualifier;

@Slf4j
public class PeersLogsQueryProcessor
    extends AbstractPeerQueryProcessor<BinaryLogRecordV1, LogPageMetadata, String>
    implements LogsQueryProcessor {
  private final OkHttpClient client;
  private final Gson gson;
  private final LogsCfg logsCfg;
  private final ExecutorService exec;

  public PeersLogsQueryProcessor(
      MemberList memberList,
      @Qualifier(HttpClientConfiguration.LOGS_OK_HTTP) OkHttpClient client,
      LogsCfg cfg,
      WhoAmI whoAmI,
      QueryCfg queryCfg,
      FanoutGrouper fanoutGrouper) {
    super(memberList, whoAmI, fanoutGrouper);
    this.client = client;
    this.gson = new Gson();
    this.logsCfg = cfg;
    this.exec = Executors.newFixedThreadPool(queryCfg.getTracesFanoutPoolSize());
  }

  protected static FilterNode toFilterNode(PageFilter<BinaryLogRecordV1, LogPageMetadata> filter) {
    if (filter instanceof LevelPageFilter lf) {
      return FilterNode.builder().kind("LEVEL").levelCode(lf.getLevelCode()).build();
    } else if (filter instanceof LogPageTraceFilter tf) {
      return FilterNode.builder().kind("TRACE").traceId(tf.getTraceId()).build();
    } else if (filter instanceof RegexPageFilter rf) {
      return FilterNode.builder().kind("REGEX").regex(rf.getRegex()).build();
    } else if (filter instanceof AndPageFilter af) {
      return FilterNode.builder()
          .kind("AND")
          .left(toFilterNode(af.getLeft()))
          .right(toFilterNode(af.getRight()))
          .build();
    } else if (filter instanceof OrPageFilter of) {
      return FilterNode.builder()
          .kind("OR")
          .left(toFilterNode(of.getLeft()))
          .right(toFilterNode(of.getRight()))
          .build();
    }
    return null;
  }

  @Override
  public List<BinaryLogRecordV1> getLogs(
      String logStream,
      long start,
      long end,
      PageFilter<BinaryLogRecordV1, LogPageMetadata> filter,
      QueryConfig cfg)
      throws IOException {
    return getPeerResults(
        LogStreamIdentifier.of(logStream), start, end, logsCfg.getIdxExpiryDuration(), filter, cfg);
  }

  /// todo: fixme - should redirect queries to nodes which are holding candidate shards and aggregate results.
  @Override
  public List<BinaryLogRecordV1> queryPeer(
      String ip,
      int port,
      StreamIdentifier<String> streamIdentifier,
      long start,
      long end,
      PageFilter<BinaryLogRecordV1, LogPageMetadata> filter,
      QueryConfig cfg) {
    try {
      log.info("query member with ip {}", ip);
      var url = String.format("http://%s:%d/logs/query/local", ip, port);
      List<BinaryLogRecordV1> out = new ArrayList<>();
      var req =
          QueryRequest.builder()
              .start(start)
              .end(end)
              .limit(1000)
              .filter(toFilterNode(filter))
              .build();
      byte[] body = gson.toJson(req).getBytes();
      Request httpReq =
          new Request.Builder()
              .url(url)
              .post(RequestBody.create(MediaType.parse("application/json"), body))
              .addHeader(Headers.SVC, streamIdentifier.getStreamId())
              .build();
      try (Response resp = client.newCall(httpReq).execute()) {
        if (!resp.isSuccessful()) {
          throw new IOException("Remote member query failed: " + resp.code());
        }
        QueryResponse qresp = gson.fromJson(resp.body().string(), QueryResponse.class);
        for (LogView v : qresp.items) out.add(BinaryLogRecordV1.fromLogView(v));
      }
      return out;
    } catch (IOException ioe) {
      throw new CompletionException(ioe);
    }
  }

  @Override
  public ExecutorService getExecutorService() {
    return exec;
  }
}
