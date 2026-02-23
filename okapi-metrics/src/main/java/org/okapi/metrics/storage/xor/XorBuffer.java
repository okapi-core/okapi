package org.okapi.metrics.storage.xor;

import java.io.IOException;
import java.io.InputStream;
import org.okapi.io.OkapiIo;
import org.okapi.io.StreamReadingException;
import org.okapi.metrics.storage.*;
import org.okapi.metrics.storage.buffers.AppendOnlyByteBuffer;
import org.okapi.metrics.storage.buffers.BufferFullException;

/**
 * XorBuffer implements semantics for storing a sequence of values. It can be used to directly store
 * double values without any additional processing.
 */
public class XorBuffer {
  ByteBufferWriter bufferWriter;
  ValueWriter valueWriter;
  int prevValBits;
  int total = 0;
  public XorBuffer(ByteBufferWriter bufferWriter) {
    this.bufferWriter = bufferWriter;
    this.valueWriter = new BitValueWriter(bufferWriter);
  }

  public static XorBuffer initialize(InputStream is, AppendOnlyByteBuffer byteBuffer)
      throws StreamReadingException, IOException {
    OkapiIo.checkMagicNumber(is, XorBufferSnapshot.MAGIC_NUMBER);
    var total = OkapiIo.readInt(is);
    var lastValueBits = OkapiIo.readInt(is);
    var writer = ByteBufferWriter.initialize(is, byteBuffer);
    OkapiIo.checkMagicNumber(is, XorBufferSnapshot.MAGIC_NUMBER_END);
    var buffer = new XorBuffer(writer);
    buffer.total = total;
    buffer.prevValBits = lastValueBits;
    return buffer;
  }

  public void push(float val) throws BufferFullException {
    // blocks
    var intVal = toInt(val);
    if (total == 0) {
      // this is the first number saved
      if (!valueWriter.canWriteBits(32)) {
        throw new BufferFullException();
      }
      valueWriter.writeInteger(intVal, 32);
    } else {
      // we are saving XOR with previous
      var xor = intVal ^ prevValBits;
      if (xor == 0) {
        if (!valueWriter.canWriteBits(1)) {
          throw new BufferFullException();
        }
        valueWriter.writeBit(false);
      } else {
        var leadZeros = Integer.numberOfLeadingZeros(xor);
        var trailZeros = Integer.numberOfTrailingZeros(xor);
        var meaningfulBits = 32 - leadZeros - trailZeros;

        if (leadZeros == Integer.numberOfLeadingZeros(prevValBits)
            && trailZeros == Integer.numberOfTrailingZeros(prevValBits)) {
          var totalBits = 32 - leadZeros - trailZeros + 2;
          if (!valueWriter.canWriteBits(totalBits)) {
            throw new BufferFullException();
          }
          valueWriter.writeBit(true);
          valueWriter.writeBit(false);
          // trim the xor with trailZeros
          var meaningfulValue = (xor >> trailZeros);
          // write the last meaningful bits
          valueWriter.writeUInt(meaningfulValue, meaningfulBits);
        } else {
          var totalBits =
              11 // 13 = 7 bits for writing meaningful bits, 6 for writing number of leading zeros
                  + (meaningfulBits) // meaningful nonzero bits plus 1 for sign
                  + 2; // 2 control bits
          if (!valueWriter.canWriteBits(totalBits)) {
            throw new BufferFullException();
          }
          // control bits
          valueWriter.writeBit(true);
          valueWriter.writeBit(true);
          // 6 bits for leading zeros
          valueWriter.writeUInt(leadZeros, 5);
          // over to use floating points instead
          // 7 bits for the number of meaningful bits
          valueWriter.writeUInt(meaningfulBits, 6);
          var meaningfulValue = (xor >> trailZeros);
          // 1 + meaningfulBits to store the number with its sign
          valueWriter.writeUInt(meaningfulValue, meaningfulBits);
        }
      }
    }
    prevValBits = intVal;
    total++;
  }

  private int toInt(float val) {
    return Float.floatToIntBits(val);
  }

  public XorBufferSnapshot snapshot() {
    return new XorBufferSnapshot(this.bufferWriter.snapshot(), total, prevValBits);
  }
}
