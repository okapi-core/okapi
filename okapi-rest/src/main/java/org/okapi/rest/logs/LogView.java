package org.okapi.rest.logs;

import lombok.Data;
import org.okapi.protos.logs.LogPayloadProto;

@Data
public class LogView {
    public long tsMillis;
    public int level;
    public String body;
    public String traceId;
    public String docId;

    public static LogView from(LogPayloadProto p) {
        LogView v = new LogView();
        v.tsMillis = p.getTsMillis();
        v.level = p.getLevel();
        v.body = p.getBody();
        v.traceId = p.hasTraceId() ? p.getTraceId() : null;
        v.docId = p.getDocId();
        return v;
    }
}
