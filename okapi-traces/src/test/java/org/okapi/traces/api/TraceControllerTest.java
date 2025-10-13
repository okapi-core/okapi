package org.okapi.traces.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.okapi.traces.service.TraceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = TraceController.class)
class TraceControllerTest {

  @Autowired MockMvc mockMvc;
  @MockitoBean TraceService traceService;

  @Test
  void ingestJson_usesTenantHeader() throws Exception {
    when(traceService.ingestOtelJson(anyString(), any())).thenReturn(2);
    String body = "{\"resourceSpans\": []}";
    mockMvc
        .perform(
            post("/v1/traces")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Okapi-Tenant-Id", "t1")
                .content(body))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.ingested").value(2));
  }

  @Test
  void listByDuration_callsService() throws Exception {
    when(traceService.listByDuration("t1", 1000L, 9000L, 3)).thenReturn(List.of());
    mockMvc
        .perform(
            get("/v1/traces/spans/by-duration")
                .header("X-Okapi-Tenant-Id", "t1")
                .param("startMillis", "1000")
                .param("endMillis", "9000")
                .param("limit", "3"))
        .andExpect(status().isOk());
    verify(traceService).listByDuration("t1", 1000L, 9000L, 3);
  }
}
