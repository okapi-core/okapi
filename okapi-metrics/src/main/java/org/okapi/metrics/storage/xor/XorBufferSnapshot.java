package org.okapi.metrics.storage.xor;

import java.io.OutputStream;
import org.okapi.io.OkapiIo;
import org.okapi.metrics.annotations.NotThreadSafe;
import org.okapi.metrics.storage.BitValueReader;
import org.okapi.metrics.storage.ByteBufferReader;
import org.okapi.metrics.storage.buffers.BufferSnapshot;

@NotThreadSafe
public class XorBufferSnapshot implements org.okapi.metrics.storage.BufferSnapshot<Float> {
  public static final String MAGIC_NUMBER = "XOR";
  public static final String MAGIC_NUMBER_END = "XOR_END";
  BufferSnapshot snapshot;
  BitValueReader bitValueReader;
  int total;
  int served;
  int previousValueBits;
  float lastValue;

  // last written value
  int lastWrittenValue;

  public XorBufferSnapshot(BufferSnapshot snapshot, int total, int lastWrittenValue) {
    this.snapshot = snapshot;
    this.total = total;
    served = 0;
    this.bitValueReader = new BitValueReader(new ByteBufferReader(snapshot));
    this.lastWrittenValue = lastWrittenValue;
  }

  public int size() {
    return total;
  }

  @Override
  public boolean hasNext() {
    return served < total;
  }

  @Override
  public void write(OutputStream os) {
    try {
      OkapiIo.writeString(os, MAGIC_NUMBER);
      OkapiIo.writeInt(os, total);
      OkapiIo.writeInt(os, lastWrittenValue);
      snapshot.write(os);
      OkapiIo.writeString(os, MAGIC_NUMBER_END);
    } catch (Exception e) {
      throw new RuntimeException("Failed to write XorBufferSnapshot", e);
    }
  }

  @Override
  public Float next() {
    if (served == 0) {
      var first = bitValueReader.readInteger(32);
      previousValueBits = first;
      lastValue = Float.intBitsToFloat(first);
      served++;
      return lastValue;
    }

    var control = bitValueReader.readBit();
    if (!control) {
      // if control is zero we're serving the same value as before
      served++;
      return lastValue;
    }

    var next = bitValueReader.readBit();
    // 10 seq -> value has the same number of leading and trailing bits
    if (!next) {
      // the next value has the same number of leading and trailing blocks
      var leadZeros = Integer.numberOfLeadingZeros(previousValueBits);
      var trailZeros = Integer.numberOfTrailingZeros(previousValueBits);
      var valBits = 32 - leadZeros - trailZeros;

      // plus 1 since we use the signed version of long
      var xorValue = bitValueReader.readUInt(valBits);
      int intBits = xorValue ^ previousValueBits;
      lastValue = Float.intBitsToFloat(intBits);
      served++;
      previousValueBits = intBits;
      return lastValue;
    } else {
      var leadZeros = bitValueReader.readUInt(5);
      var meaningfulBits = bitValueReader.readUInt(6);
      var trail = 32 - leadZeros - meaningfulBits;

      // again plus 1 since we use the signed version of longs
      var meaningfulValue = bitValueReader.readUInt(meaningfulBits);
      meaningfulValue = meaningfulValue << trail;
      var valueBits = previousValueBits ^ meaningfulValue;
      lastValue = Float.intBitsToFloat(valueBits);
      served++;
      previousValueBits = valueBits;
      return lastValue;
    }
  }
}
