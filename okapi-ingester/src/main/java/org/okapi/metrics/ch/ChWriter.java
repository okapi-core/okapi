/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.metrics.ch;

import com.clickhouse.client.api.Client;
import com.clickhouse.client.api.insert.InsertResponse;
import com.clickhouse.data.ClickHouseFormat;
import com.google.gson.Gson;
import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Future;

public class ChWriter {
  private final Client client;
  private final Gson gson = new Gson();
  private static final DateTimeFormatter TS_FMT =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS").withZone(ZoneOffset.UTC);

  public ChWriter(Client client) {
    this.client = client;
  }

  public String toJsonEachRow(List<String> rows) {
    var sb = new StringBuilder();
    for (var r : rows) {
      sb.append(r).append('\n');
    }
    return sb.toString();
  }

  public Future<InsertResponse> writeRows(String table, List<String> jsonRows) {
    var rows = toJsonEachRow(jsonRows).getBytes();
    var bis = new ByteArrayInputStream(rows);
    return client.insert(table, bis, ClickHouseFormat.JSONEachRow);
  }

  public Future<InsertResponse> writeHistoSamplesBinary(List<ChHistoSampleInsertRow> rows) {
    var jsonRows =
        rows.stream()
            .map(
                r -> {
                  var map = new HashMap<String, Object>();
                  map.put("resource", r.getResource());
                  map.put("metric_name", r.getMetric_name());
                  map.put("tags", r.getTags());
                  map.put("ts_start", TS_FMT.format(Instant.ofEpochMilli(r.getTs_start_ms())));
                  map.put("ts_end", TS_FMT.format(Instant.ofEpochMilli(r.getTs_end_ms())));
                  map.put("buckets", r.getBuckets());
                  map.put("counts", r.getCounts());
                  map.put("histo_type", r.getHisto_type());
                  return gson.toJson(map);
                })
            .toList();
    var data = toJsonEachRow(jsonRows).getBytes();
    var bis = new ByteArrayInputStream(data);
    return client.insert(ChConstants.TBL_HISTOS, bis, ClickHouseFormat.JSONEachRow);
  }
}
