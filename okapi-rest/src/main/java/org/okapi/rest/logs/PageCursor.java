package org.okapi.rest.logs;

import org.okapi.protos.logs.LogPayloadProto;

import java.util.Base64;

public class PageCursor {
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

    public static PageCursor from(LogPayloadProto p) {
        return new PageCursor(
                p.getTsMillis(),
                p.getLevel(),
                p.getBody() == null ? 0 : p.getBody().hashCode(),
                p.getTraceId());
    }

    public String encode() {
        String raw = ts + "|" + level + "|" + bodyHash + "|" + traceId;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes());
    }

    public static PageCursor decode(String token) {
        String raw = new String(Base64.getUrlDecoder().decode(token));
        String[] parts = raw.split("\\|", 4);
        return new PageCursor(
                Long.parseLong(parts[0]),
                Integer.parseInt(parts[1]),
                Integer.parseInt(parts[2]),
                parts.length > 3 ? parts[3] : "");
    }

    public static int compare(LogPayloadProto p, PageCursor c) {
        if (p.getTsMillis() != c.ts) return Long.compare(p.getTsMillis(), c.ts);
        if (p.getLevel() != c.level) return Integer.compare(p.getLevel(), c.level);
        int bh = p.getBody() == null ? 0 : p.getBody().hashCode();
        if (bh != c.bodyHash) return Integer.compare(bh, c.bodyHash);
        String tid = p.hasTraceId() ? p.getTraceId() : "";
        return tid.compareTo(c.traceId);
    }
}