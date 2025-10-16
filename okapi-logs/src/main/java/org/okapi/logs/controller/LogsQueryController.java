package org.okapi.logs.controller;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.okapi.logs.query.*;
import org.okapi.protos.logs.LogPayloadProto;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class LogsQueryController {
  private final MultiSourceQueryProcessor processor;

  @PostMapping(path = "/logs/query", consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public QueryResponse query(
      @RequestHeader("X-Okapi-Tenant-Id") String tenantId,
      @RequestHeader("X-Okapi-Log-Stream") String logStream,
      @RequestBody QueryRequest req) throws Exception {
    LogFilter filter = toFilter(req.filter);
    List<LogPayloadProto> all =
        processor.getLogs(tenantId, logStream, req.start, req.end, filter);

    // Sort by ts_millis then tie-breaker (level, body hash, traceId)
    Comparator<LogPayloadProto> cmp = Comparator
        .comparingLong(LogPayloadProto::getTsMillis)
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

  @Data
  public static class QueryRequest {
    public long start;
    public long end;
    public int limit;
    public String pageToken;
    public FilterNode filter;
  }

  @Data
  public static class FilterNode {
    public String kind; // LEVEL, TRACE, REGEX, AND, OR
    public String regex;
    public String traceId;
    public Integer levelCode;
    public FilterNode left;
    public FilterNode right;
  }

  @Data
  public static class QueryResponse {
    public List<LogView> items;
    public String nextPageToken;
  }

  @Data
  public static class LogView {
    public long tsMillis;
    public int level;
    public String body;
    public String traceId;

    public static LogView from(LogPayloadProto p) {
      LogView v = new LogView();
      v.tsMillis = p.getTsMillis();
      v.level = p.getLevel();
      v.body = p.getBody();
      v.traceId = p.hasTraceId() ? p.getTraceId() : null;
      return v;
    }
  }

  static class PageCursor {
    final long ts;
    final int level;
    final int bodyHash;
    final String traceId;

    PageCursor(long ts, int level, int bodyHash, String traceId) {
      this.ts = ts;
      this.level = level;
      this.bodyHash = bodyHash;
      this.traceId = traceId == null ? "" : traceId;
    }

    static PageCursor from(LogPayloadProto p) {
      return new PageCursor(p.getTsMillis(), p.getLevel(),
          p.getBody() == null ? 0 : p.getBody().hashCode(), p.getTraceId());
    }

    String encode() {
      String raw = ts + "|" + level + "|" + bodyHash + "|" + traceId;
      return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes());
    }

    static PageCursor decode(String token) {
      String raw = new String(Base64.getUrlDecoder().decode(token));
      String[] parts = raw.split("\\|", 4);
      return new PageCursor(Long.parseLong(parts[0]), Integer.parseInt(parts[1]),
          Integer.parseInt(parts[2]), parts.length > 3 ? parts[3] : "");
    }

    static int compare(LogPayloadProto p, PageCursor c) {
      if (p.getTsMillis() != c.ts) return Long.compare(p.getTsMillis(), c.ts);
      if (p.getLevel() != c.level) return Integer.compare(p.getLevel(), c.level);
      int bh = p.getBody() == null ? 0 : p.getBody().hashCode();
      if (bh != c.bodyHash) return Integer.compare(bh, c.bodyHash);
      String tid = p.hasTraceId() ? p.getTraceId() : "";
      return tid.compareTo(c.traceId);
    }
  }
}

