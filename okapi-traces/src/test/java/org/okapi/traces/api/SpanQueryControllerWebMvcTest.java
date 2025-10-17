package org.okapi.traces.api;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.google.protobuf.ByteString;
import io.opentelemetry.proto.trace.v1.Span;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.okapi.traces.query.AttributeFilter;
import org.okapi.traces.query.TraceQueryProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SpanQueryController.class)
public class SpanQueryControllerWebMvcTest {

  @Autowired MockMvc mvc;
  @MockitoBean TraceQueryProcessor processor;

  private static Span makeSpan(String trace, String span, long startMs) {
    return Span.newBuilder()
        .setTraceId(ByteString.copyFrom(java.util.HexFormat.of().parseHex(trace)))
        .setSpanId(ByteString.copyFrom(java.util.HexFormat.of().parseHex(span)))
        .setStartTimeUnixNano(startMs * 1_000_000L)
        .setEndTimeUnixNano((startMs + 1) * 1_000_000L)
        .build();
  }

  @Test
  void query_by_traceId_spanId_attribute_and_invalid() throws Exception {
    var s1 = makeSpan("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", "1111111111111111", 10);
    var s2 = makeSpan("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", "2222222222222222", 20);
    when(processor.getSpansWithFilter(
            anyLong(), anyLong(), anyString(), anyString(), eq("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa")))
        .thenReturn(List.of(s1, s2));
    when(processor.getTrace(anyLong(), anyLong(), anyString(), anyString(), eq("1111111111111111")))
        .thenReturn(List.of(s1));
    when(processor.getSpansWithFilter(
            anyLong(), anyLong(), anyString(), anyString(), any(AttributeFilter.class)))
        .thenReturn(List.of(s2));

    // traceId
    mvc.perform(
            post("/span/query")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Okapi-Tenant-Id", "t")
                .header("X-Okapi-App", "a")
                .content(
                    "{\"startMillis\":0,\"endMillis\":1000,\"traceId\":\"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.spans", hasSize(2)))
        .andExpect(jsonPath("$.spans[0].spanId", notNullValue()));

    // spanId
    mvc.perform(
            post("/span/query")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Okapi-Tenant-Id", "t")
                .header("X-Okapi-App", "a")
                .content("{\"startMillis\":0,\"endMillis\":1000,\"spanId\":\"1111111111111111\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.spans[0].spanId", notNullValue()));

    // attribute filter pattern
    mvc.perform(
            post("/span/query")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Okapi-Tenant-Id", "t")
                .header("X-Okapi-App", "a")
                .content(
                    "{\"startMillis\":0,\"endMillis\":1000,\"attributeFilter\":{\"name\":\"service.name\",\"pattern\":\"pay.*\"}}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.spans[0].spanId", notNullValue()));

    // invalid request
    mvc.perform(
            post("/span/query")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Okapi-Tenant-Id", "t")
                .header("X-Okapi-App", "a")
                .content("{\"startMillis\":0,\"endMillis\":1000}"))
        .andExpect(status().isBadRequest());

    // missing headers
    mvc.perform(
            post("/span/query")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"startMillis\":0,\"endMillis\":1000,\"traceId\":\"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\"}"))
        .andExpect(status().isBadRequest());
  }
}
