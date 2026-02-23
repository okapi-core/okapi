/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.traces.query;

import com.google.common.base.Preconditions;
import com.google.gson.Gson;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.okapi.abstractfilter.AndPageFilter;
import org.okapi.abstractfilter.OrPageFilter;
import org.okapi.abstractfilter.PageFilter;
import org.okapi.abstractio.LogStreamIdentifier;
import org.okapi.header.Headers;
import org.okapi.identity.MemberList;
import org.okapi.identity.WhoAmI;
import org.okapi.logs.query.*;
import org.okapi.primitives.BinarySpanRecordV2;
import org.okapi.queryproc.AbstractPeerQueryProcessor;
import org.okapi.queryproc.FanoutGrouper;
import org.okapi.queryproc.TraceQueryProcessor;
import org.okapi.rest.traces.SpanFilterRest;
import org.okapi.rest.traces.SpanQueryRequest;
import org.okapi.rest.traces.SpanQueryResponse;
import org.okapi.spring.configs.properties.QueryCfg;
import org.okapi.streams.StreamIdentifier;
import org.okapi.traces.config.TracesCfg;
import org.okapi.traces.io.SpanPageMetadata;

@Slf4j
public class PeersTraceQueryProcessor
    extends AbstractPeerQueryProcessor<BinarySpanRecordV2, SpanPageMetadata, String>
    implements TraceQueryProcessor {

  ExecutorService executorService;
  TracesCfg tracesCfg;
  Gson gson;
  OkHttpClient client;

  public PeersTraceQueryProcessor(
      TracesCfg tracesCfg,
      MemberList memberList,
      WhoAmI whoAmI,
      QueryCfg cfg,
      OkHttpClient client,
      FanoutGrouper fanoutGrouper) {
    super(memberList, whoAmI, fanoutGrouper);
    this.tracesCfg = Preconditions.checkNotNull(tracesCfg);
    this.gson = new Gson();
    this.executorService = Executors.newFixedThreadPool(cfg.getTracesFanoutPoolSize());
    this.client = client;
  }

  protected static SpanFilterRest toFilterNode(
      PageFilter<BinarySpanRecordV2, SpanPageMetadata> filter) {
    if (filter instanceof SpanPageTraceFilter tf) {
      return SpanFilterRest.builder().kind("LEVEL").traceId(tf.traceId).build();
    } else if (filter instanceof AndPageFilter af) {
      return SpanFilterRest.builder()
          .kind("AND")
          .left(toFilterNode(af.getLeft()))
          .right(toFilterNode(af.getRight()))
          .build();
    } else if (filter instanceof OrPageFilter of) {
      return SpanFilterRest.builder()
          .kind("OR")
          .left(toFilterNode(of.getLeft()))
          .right(toFilterNode(of.getRight()))
          .build();
    }
    return null;
  }

  @Override
  public List<BinarySpanRecordV2> queryPeer(
      String ip,
      int port,
      StreamIdentifier<String> streamIdentifier,
      long start,
      long end,
      PageFilter<BinarySpanRecordV2, SpanPageMetadata> filter,
      QueryConfig cfg) {
    try {
      var url = String.format("http://%s:%d/traces/query/local", ip, port);
      List<BinarySpanRecordV2> out = new ArrayList<>();
      var req =
          SpanQueryRequest.builder()
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
        var qresp = gson.fromJson(resp.body().string(), SpanQueryResponse.class);
        for (var spanDto : qresp.getItems()) out.add(BinarySpanRecordV2.fromSpanDto(spanDto));
      }
      return out;
    } catch (IOException ioe) {
      throw new CompletionException(ioe);
    }
  }

  @Override
  public ExecutorService getExecutorService() {
    return executorService;
  }

  @Override
  public List<BinarySpanRecordV2> getTraces(
      String app,
      long start,
      long end,
      PageFilter<BinarySpanRecordV2, SpanPageMetadata> filter,
      QueryConfig cfg)
      throws Exception {
    return getPeerResults(
        LogStreamIdentifier.of(app), start, end, tracesCfg.getIdxExpiryDuration(), filter, cfg);
  }
}
