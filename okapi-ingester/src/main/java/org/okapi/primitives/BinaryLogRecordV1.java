package org.okapi.primitives;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.okapi.io.OkapiIo;
import org.okapi.io.StreamReadingException;
import org.okapi.queryproc.IdentifiableDocument;
import org.okapi.rest.logs.LogView;

@Builder
@AllArgsConstructor
public class BinaryLogRecordV1 implements IdentifiableDocument {
  @Getter String docId;
  @Getter String service;
  @Getter long tsMillis;
  @Getter int level;
  @Getter String traceId = "";
  @Getter String body;

  private BinaryLogRecordV1() {}

  public static BinaryLogRecordV1 fromLogView(LogView logView) {
    return BinaryLogRecordV1.builder()
        .docId(logView.getDocId())
        .tsMillis(logView.getTsMillis())
        .level(logView.getLevel())
        .traceId(logView.getTraceId())
        .body(logView.getBody())
        .service(logView.getService())
        .build();
  }

  public static BinaryLogRecordV1 fromByteArray(byte[] bytes) {
    var logRecord = new BinaryLogRecordV1();
    var bis = new ByteArrayInputStream(bytes);
    try {
      logRecord.setDocId(OkapiIo.readString(bis));
      logRecord.setDocId(OkapiIo.readString(bis));
      logRecord.setTsMillis(OkapiIo.readLong(bis));
      logRecord.setLevel(OkapiIo.readInt(bis));
      logRecord.setTraceId(OkapiIo.readString(bis));
      logRecord.setBody(OkapiIo.readString(bis));
      return logRecord;
    } catch (IOException | StreamReadingException e) {
      throw new RuntimeException(e);
    }
  }

  public LogView toLogView() {
    return LogView.builder()
        .docId(docId)
        .tsMillis(tsMillis)
        .level(level)
        .traceId(traceId)
        .body(body)
        .build();
  }

  private void setDocId(String docId) {
    this.docId = docId;
  }

  private void setTsMillis(long tsMillis) {
    this.tsMillis = tsMillis;
  }

  private void setLevel(int level) {
    this.level = level;
  }

  private void setBody(String body) {
    this.body = body;
  }

  private void setTraceId(String traceId) {
    this.traceId = traceId;
  }

  public int getSerializedSize() {
    return getDocIdSize()
        + /* UUID */ +8 /* ts */
        + 4 /* level */
        + getTraceIdSerializedSize()
        + getBodySerializedSize();
  }

  public int getDocIdSize() {
    return docId.getBytes().length;
  }

  public int getTraceIdSerializedSize() {
    if (traceId == null) {
      return 4;
    }
    return 4 + traceId.getBytes().length;
  }

  public int getBodySerializedSize() {
    return 4 + body.getBytes().length;
  }

  public byte[] toByteArray() throws IOException {
    var os = new ByteArrayOutputStream();
    try {
      OkapiIo.writeString(os, docId);
      OkapiIo.writeString(os, service);
      OkapiIo.writeLong(os, tsMillis);
      OkapiIo.writeInt(os, level);
      OkapiIo.writeString(os, traceId);
      OkapiIo.writeString(os, body);
      return os.toByteArray();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
