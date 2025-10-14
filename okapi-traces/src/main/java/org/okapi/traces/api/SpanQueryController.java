package org.okapi.traces.api;

import com.google.protobuf.util.JsonFormat;
import io.opentelemetry.proto.trace.v1.Span;
import java.util.List;
import java.util.Map;
import org.okapi.traces.query.AttributeFilter;
import org.okapi.traces.query.TraceQueryProcessor;
import org.okapi.traces.query.TraceQueryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class SpanQueryController {

  public static class SpanQueryRequest {
    public String tenantId;
    public String application;
    public long startMillis;
    public long endMillis;
    public String traceId;
    public String spanId;
    public Attribute attributeFilter;

    public static class Attribute {
      public String name;
      public String value;
      public String pattern;
    }
  }

  @Autowired private TraceQueryProcessor multiplexingTraceQueryProcessor;

  @PostMapping(value = "/span/query", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<String> query(
      @RequestHeader("X-Okapi-Tenant-Id") String tenantFromHeader,
      @RequestHeader("X-Okapi-App") String appFromHeader,
      @RequestBody SpanQueryRequest reqBody) throws Exception {
    String tenant = tenantFromHeader;
    String app = appFromHeader;
    validate(tenant, app);

    long start = reqBody.startMillis;
    long end = reqBody.endMillis;

    List<Span> spans;
    if (reqBody.traceId != null && !reqBody.traceId.isBlank()) {
      spans = multiplexingTraceQueryProcessor.getSpans(start, end, tenant, app, reqBody.traceId);
    } else if (reqBody.spanId != null && !reqBody.spanId.isBlank()) {
      spans = multiplexingTraceQueryProcessor.getTrace(start, end, tenant, app, reqBody.spanId);
    } else if (reqBody.attributeFilter != null) {
      var af = reqBody.attributeFilter;
      AttributeFilter filter =
          (af.pattern != null && !af.pattern.isBlank())
              ? AttributeFilter.withPattern(af.name, af.pattern)
              : new AttributeFilter(af.name, af.value);
      spans = multiplexingTraceQueryProcessor.getSpans(start, end, tenant, app, filter);
    } else {
      return ResponseEntity.badRequest().body("{\"error\":\"Invalid query\"}");
    }

    // Serialize as JSON array of OTel Spans
    var printer = JsonFormat.printer();
    StringBuilder sb = new StringBuilder();
    sb.append("[");
    for (int i = 0; i < spans.size(); i++) {
      String json = printer.print(spans.get(i));
      sb.append(json);
      if (i < spans.size() - 1) sb.append(',');
    }
    sb.append("]");
    return ResponseEntity.ok(sb.toString());
  }

  private static void validate(String tenant, String app) {
    if (tenant == null || tenant.isBlank()) throw new IllegalArgumentException("Missing X-Okapi-Tenant-Id");
    if (app == null || app.isBlank()) throw new IllegalArgumentException("Missing X-Okapi-App");
  }
}

