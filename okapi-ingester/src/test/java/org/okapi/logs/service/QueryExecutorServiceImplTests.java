/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.logs.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.okapi.logs.query.QueryConfig;
import org.okapi.logs.query.processor.MultiSourceLogsQueryProcessor;
import org.okapi.rest.logs.FilterNode;
import org.okapi.rest.logs.QueryRequest;

public class QueryExecutorServiceImplTests {

  @Test
  void testAllSources() throws Exception {
    var proc = mock(MultiSourceLogsQueryProcessor.class);
    var qe = new QueryExecutorServiceImpl(proc);
    var req =
        QueryRequest.builder()
            .start(0)
            .end(10)
            .limit(10)
            .filter(FilterNode.builder().kind("LEVEL").levelCode(2).build())
            .build();
    qe.queryAllSources("s", req, 10);
    verify(proc, times(1)).getLogs(eq("s"), eq(0L), eq(10L), any(), eq(QueryConfig.allSources()));
  }

  @Test
  void testDiskAndBufferPool() throws Exception {
    var proc = mock(MultiSourceLogsQueryProcessor.class);
    var qe = new QueryExecutorServiceImpl(proc);
    var req =
        QueryRequest.builder()
            .start(0)
            .end(10)
            .limit(10)
            .filter(FilterNode.builder().kind("LEVEL").levelCode(2).build())
            .build();
    qe.queryDiskAndBufferPool("s", req, 10);
    verify(proc, times(1)).getLogs(eq("s"), eq(0L), eq(10L), any(), eq(QueryConfig.localSources()));
  }
}
