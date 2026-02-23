package org.okapi.primitives;

import com.google.protobuf.ByteString;
import io.opentelemetry.proto.common.v1.AnyValue;
import io.opentelemetry.proto.common.v1.KeyValue;
import io.opentelemetry.proto.trace.v1.Span;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.codec.binary.Hex;
import org.okapi.io.OkapiIo;
import org.okapi.queryproc.IdentifiableDocument;
import org.okapi.rest.traces.SpanDto;
import org.okapi.traces.io.SpanIngestionRecord;

@AllArgsConstructor
public class BinarySpanRecordV2 implements IdentifiableDocument {
  @Getter String svc;
  @Getter Span span;

  public BinarySpanRecordV2(Span span) {
    this("default", span);
  }

  public static final BinarySpanRecordV2 fromIngestionRecord(SpanIngestionRecord ingestionRecord) {
    return new BinarySpanRecordV2(ingestionRecord.getSvc(), ingestionRecord.getSpan());
  }

  public static final BinarySpanRecordV2 fromByteArray(byte[] data) {
    try {
      var bis = new ByteArrayInputStream(data);
      var svc = OkapiIo.readString(bis);
      var bytes = OkapiIo.readBytes(bis);
      Span span = Span.parseFrom(bytes);
      return new BinarySpanRecordV2(svc, span);
    } catch (Exception e) {
      throw new RuntimeException("Failed to parse Span from byte array", e);
    }
  }

  public byte[] toByteArray() throws IOException {
    var bos = new ByteArrayOutputStream();
    OkapiIo.writeString(bos, svc);
    OkapiIo.writeBytes(bos, span.toByteArray());
    return bos.toByteArray();
  }

  public static final BinarySpanRecordV2 fromSpanDto(SpanDto dto) {
    var builder =
        Span.newBuilder()
            .setSpanId(
                dto.getSpanId() != null ? ByteString.copyFrom(dto.getSpanId()) : ByteString.EMPTY)
            .setTraceId(
                dto.getTraceId() != null ? ByteString.copyFrom(dto.getTraceId()) : ByteString.EMPTY)
            .setName(dto.getName() != null ? dto.getName() : "")
            .setStartTimeUnixNano(dto.getStartTimeMillis() * 1_000_000)
            .setEndTimeUnixNano(dto.getEndTimeMillis() * 1_000_000)
            .setParentSpanId(
                dto.getParentSpanId() != null
                    ? ByteString.copyFrom(dto.getParentSpanId())
                    : ByteString.EMPTY);
    for (var attr : dto.getAttributes().entrySet()) {
      builder.addAttributes(
          KeyValue.newBuilder()
              .setKey(attr.getKey())
              .setValue(AnyValue.newBuilder().setStringValue(attr.getValue()).build())
              .build());
    }
    return new BinarySpanRecordV2(dto.getSvc(), builder.build());
  }

  public static final SpanDto toSpanDto(BinarySpanRecordV2 record) {
    var span = record.getSpan();
    var dto =
        SpanDto.builder()
            .spanId(span.getSpanId().toByteArray())
            .traceId(span.getTraceId().toByteArray())
            .startTimeMillis(span.getStartTimeUnixNano() / 1_000_00)
            .endTimeMillis(span.getEndTimeUnixNano() / 1_000_000)
            .parentSpanId(span.getParentSpanId().toByteArray())
            .build();
    for (var attr : span.getAttributesList()) {
      dto.getAttributes().put(attr.getKey(), attr.getValue().getStringValue());
    }
    return dto;
  }

  @Override
  public String getDocId() {
    return Hex.encodeHexString(span.getSpanId().toByteArray());
  }
}
