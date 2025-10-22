package org.okapi.logs.controller;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.okapi.logs.query.*;
import org.okapi.protos.logs.LogPayloadProto;
import org.okapi.rest.logs.*;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class LogsQueryController {
  private final MultiSourceQueryProcessor processor;

  @PostMapping(
      path = "/logs/query",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public QueryResponse query(
      @RequestHeader("X-Okapi-Tenant-Id") String tenantId,
      @RequestHeader("X-Okapi-Log-Stream") String logStream,
      @RequestBody QueryRequest req)
      throws Exception {

    LogFilter filter = toFilter(req.filter);
    List<LogPayloadProto> all = processor.getLogs(tenantId, logStream, req.start, req.end, filter);

    // Sort by ts_millis then tie-breaker (level, body hash, traceId)
    Comparator<LogPayloadProto> cmp =
        Comparator.comparingLong(LogPayloadProto::getTsMillis)
            .thenComparingInt(LogPayloadProto::getLevel)
            .thenComparing(p -> p.getBody(), Comparator.nullsFirst(Comparator.naturalOrder()))
            .thenComparing(p -> p.getTraceId(), Comparator.nullsFirst(Comparator.naturalOrder()));
    all.sort(cmp);

    // Apply pagination using pageToken: base64 of "ts|level|hash(body)|traceId"
    int startIdx = 0;
    if (req.pageToken != null && !req.pageToken.isEmpty()) {
      PageCursor c = PageCursor.decode(req.pageToken);
      for (int i = 0; i < all.size(); i++) {
        LogPayloadProto p = all.get(i);
        if (PageCursor.compare(p, c) > 0) {
          startIdx = i;
          break;
        }
      }
    }
    int limit = Math.max(1, Math.min(req.limit <= 0 ? 100 : req.limit, 1000));
    int endIdx = Math.min(all.size(), startIdx + limit);
    List<LogView> items = new ArrayList<>();
    for (int i = startIdx; i < endIdx; i++) {
      LogPayloadProto p = all.get(i);
      items.add(LogView.from(p));
    }

    String nextToken = null;
    if (endIdx < all.size()) {
      nextToken = PageCursor.from(all.get(endIdx - 1)).encode();
    }
    QueryResponse resp = new QueryResponse();
    resp.items = items;
    resp.nextPageToken = nextToken;
    return resp;
  }

  private static LogFilter toFilter(FilterNode n) {
    if (n == null) return new RegexFilter(".*");
    return switch (n.kind) {
      case "LEVEL" -> new LevelFilter(n.levelCode);
      case "TRACE" -> new TraceFilter(n.traceId);
      case "REGEX" -> new RegexFilter(Objects.requireNonNullElse(n.regex, ".*"));
      case "AND" -> new AndFilter(toFilter(n.left), toFilter(n.right));
      case "OR" -> new OrFilter(toFilter(n.left), toFilter(n.right));
      default -> new RegexFilter(".*");
    };
  }
}
