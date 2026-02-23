/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.metrics.query;

import com.google.gson.Gson;
import java.util.List;
import java.util.Map;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.okapi.identity.MemberList;
import org.okapi.identity.WhoAmI;
import org.okapi.metrics.pojos.RES_TYPE;
import org.okapi.metrics.query.rest.GaugeScanQuery;
import org.okapi.metrics.query.rest.HistogramScanQuery;
import org.okapi.primitives.ReadonlyHistogram;
import org.okapi.primitives.TimestampedReadonlySketch;

public class MetricsClientImpl implements MetricsClient {
  OkHttpClient okHttpClient;
  MemberList memberList;
  WhoAmI whoAmI;
  Gson gson;

  public MetricsClientImpl(OkHttpClient okHttpClient, MemberList memberList, WhoAmI whoAmI) {
    this.okHttpClient = okHttpClient;
    this.memberList = memberList;
    this.whoAmI = whoAmI;
    this.gson = new Gson();
  }

  @Override
  public List<TimestampedReadonlySketch> queryGaugeSketches(
      String nodeId,
      String metricName,
      Map<String, String> paths,
      RES_TYPE resType,
      long qStart,
      long qEnd) {
    if (nodeId.equals(whoAmI.getNodeId())) {
      return List.of();
    }
    var member = memberList.getMember(nodeId);
    var gaugeScanQuery = new GaugeScanQuery(metricName, paths, qStart, qEnd, resType);
    var body = gson.toJson(gaugeScanQuery);
    var request =
        new Request.Builder()
            .url(
                "http://"
                    + member.getIp()
                    + ":"
                    + member.getPort()
                    + "/metrics/query/gaugeSketches")
            .post(RequestBody.create(body.getBytes()))
            .build();
    try (var res = okHttpClient.newCall(request).execute()) {
      if (!res.isSuccessful()) {
        throw new RuntimeException("Failed to query gauge sketches from node " + nodeId);
      }
      var respBody = res.body().string();
      return List.of(gson.fromJson(respBody, TimestampedReadonlySketch[].class));
    } catch (Exception e) {
      throw new RuntimeException("Error querying gauge sketches from node " + nodeId, e);
    }
  }

  @Override
  public List<ReadonlyHistogram> queryHistograms(
      String nodeId, String metricName, Map<String, String> paths, long qStart, long qEnd) {
    if (nodeId.equals(whoAmI.getNodeId())) {
      return List.of();
    }
    var member = memberList.getMember(nodeId);
    var gaugeScanQuery = new HistogramScanQuery(metricName, paths, qStart, qEnd);
    var body = gson.toJson(gaugeScanQuery);
    var request =
        new Request.Builder()
            .url(
                "http://"
                    + member.getIp()
                    + ":"
                    + member.getPort()
                    + "/metrics/query/histoSketches")
            .post(RequestBody.create(body.getBytes()))
            .build();
    try (var res = okHttpClient.newCall(request).execute()) {
      if (!res.isSuccessful()) {
        throw new RuntimeException("Failed to query gauge sketches from node " + nodeId);
      }
      var respBody = res.body().string();
      return List.of(gson.fromJson(respBody, ReadonlyHistogram[].class));
    } catch (Exception e) {
      throw new RuntimeException("Error querying gauge sketches from node " + nodeId, e);
    }
  }
}
