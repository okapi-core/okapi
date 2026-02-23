package org.okapi.primitives;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import lombok.Getter;
import lombok.SneakyThrows;
import org.okapi.io.OkapiIo;
import org.okapi.io.StreamReadingException;

@Getter
public class PrimitiveValue implements RawSerializable {
  TYPE type;
  String stringValue;
  Double doubleValue;
  Long longValue;
  Boolean booleanValue;

  public static PrimitiveValue fromStream(byte[] bytes) throws StreamReadingException, IOException {
    var val = new PrimitiveValue();
    val.fromByteArray(bytes, 0, bytes.length);
    return val;
  }

  @SneakyThrows
  public byte[] toBytes() {
    var out = new ByteArrayOutputStream();

    var type =
        switch (this.type) {
          case STRING -> this.stringValue.length(); // approximate assuming 1 byte per char
          case DOUBLE -> 2;
          case LONG -> 3;
          case BOOLEAN -> 4;
        };

    OkapiIo.writeInt(out, type);
    switch (this.type) {
      case STRING -> OkapiIo.writeString(out, stringValue);
      case DOUBLE -> OkapiIo.writeDouble(out, doubleValue);
      case LONG -> OkapiIo.writeLong(out, longValue);
      case BOOLEAN -> OkapiIo.writeBoolean(out, booleanValue);
    }

    return out.toByteArray();
  }

  private PrimitiveValue() {}

  private void checkType() {
    if (type != null) throw new IllegalStateException("Type already set to " + type);
  }

  private void setType(TYPE type) {
    checkType();
    this.type = type;
  }

  public void setStringValue(String stringValue) {
    checkType();
    setType(TYPE.STRING);
    this.stringValue = stringValue;
  }

  public void setDoubleValue(Double doubleValue) {
    checkType();
    setType(TYPE.DOUBLE);
    this.doubleValue = doubleValue;
  }

  public void setLongValue(Long longValue) {
    checkType();
    setType(TYPE.LONG);
    this.longValue = longValue;
  }

  public void setBooleanValue(Boolean booleanValue) {
    checkType();
    setType(TYPE.BOOLEAN);
    this.booleanValue = booleanValue;
  }

  public int getSize() {
    return switch (this.type) {
      case STRING -> stringValue.length();
      case DOUBLE -> 8;
      case LONG -> 8;
      case BOOLEAN -> 4;
    };
  }

  @Override
  public void fromByteArray(byte[] bytes, int offset, int len)
      throws StreamReadingException, IOException {
    var is = new ByteArrayInputStream(bytes, offset, len);
    var type = OkapiIo.readInt(is);
    var val = new PrimitiveValue();
    switch (type) {
      case 1 -> {
        val.setStringValue(OkapiIo.readString(is));
      }
      case 2 -> {
        val.setDoubleValue(OkapiIo.readDouble(is));
      }
      case 3 -> {
        val.setLongValue(OkapiIo.readLong(is));
      }
      case 4 -> {
        val.setBooleanValue(OkapiIo.readBoolean(is));
      }
      default -> throw new StreamReadingException("Unknown PrimitiveValue type " + type);
    }
  }

  @Override
  public int byteSize() {
    return switch (type) {
      case STRING -> 0;
      case DOUBLE -> 8;
      case LONG -> 8;
      case BOOLEAN -> 4;
    };
  }

  @Override
  public byte[] toByteArray() {
    return this.toBytes();
  }

  enum TYPE {
    STRING,
    DOUBLE,
    LONG,
    BOOLEAN
  }
}
