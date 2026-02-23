package org.okapi.traces.io;

import io.opentelemetry.proto.trace.v1.Span;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.okapi.io.OkapiIo;
import org.okapi.io.StreamReadingException;

@AllArgsConstructor
@Getter
public class SpanIngestionRecord {
  String svc;
  Span span;

  public byte[] toByteArray() throws IOException {
    var bos = new ByteArrayOutputStream();
    OkapiIo.writeString(bos, svc);
    OkapiIo.writeBytes(bos, span.toByteArray());
    return bos.toByteArray();
  }

  public static SpanIngestionRecord from(String svc, Span span) {
    return new SpanIngestionRecord(svc, span);
  }

  public static SpanIngestionRecord fromByteArray(byte[] bytes)
      throws StreamReadingException, IOException {
    var bis = new ByteArrayInputStream(bytes);
    var svc = OkapiIo.readString(bis);
    var spanBytes = OkapiIo.readBytes(bis);
    var span = Span.parseFrom(spanBytes);
    return new SpanIngestionRecord(svc, span);
  }
}
