package org.okapi.traces.api;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.google.protobuf.util.JsonFormat;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import io.opentelemetry.proto.trace.v1.ResourceSpans;
import io.opentelemetry.proto.trace.v1.ScopeSpans;
import io.opentelemetry.proto.trace.v1.Span;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.okapi.traces.service.TraceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(IngestionController.class)
public class IngestionControllerWebMvcTest {

  @Autowired MockMvc mvc;
  @MockitoBean TraceService traceService;

  private static String otlpJson() throws Exception {
    long now = 1700000000000L;
    var sp =
        Span.newBuilder()
            .setTraceId(com.google.protobuf.ByteString.copyFrom(new byte[16]))
            .setSpanId(com.google.protobuf.ByteString.copyFrom(new byte[8]))
            .setStartTimeUnixNano(now * 1_000_000L)
            .setEndTimeUnixNano((now + 1) * 1_000_000L)
            .build();
    var scope = ScopeSpans.newBuilder().addSpans(sp).build();
    var rs = ResourceSpans.newBuilder().addScopeSpans(scope).build();
    var req = ExportTraceServiceRequest.newBuilder().addResourceSpans(rs).build();
    return JsonFormat.printer().print(req);
  }

  @Test
  void json_ingest_ok_and_header_validation() throws Exception {
    Mockito.when(traceService.ingestOtelProtobuf(Mockito.any(), Mockito.any(), Mockito.any()))
        .thenReturn(1);

    mvc.perform(
            post("/v1/traces")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Okapi-Tenant-Id", "t")
                .header("X-Okapi-App", "a")
                .content(otlpJson()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.ingested", is(1)));

    // Missing headers -> 400
    mvc.perform(
            post("/v1/traces")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Okapi-App", "a")
                .content(otlpJson()))
        .andExpect(status().isBadRequest());

    mvc.perform(
            post("/v1/traces")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Okapi-Tenant-Id", "t")
                .content(otlpJson()))
        .andExpect(status().isBadRequest());
  }
}
