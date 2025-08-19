package org.okapi.metrics.codec;

import org.okapi.metrics.storage.ValueReader;
import org.okapi.metrics.storage.ValueWriter;
import org.okapi.metrics.storage.buffers.BufferFullException;

public class GorillaCodec {

  public static boolean writeInteger(int X, ValueWriter writer) throws BufferFullException {
    if (X == 0) {
      if(!writer.canWriteBits(1)){
        throw new BufferFullException();
      }
      writer.writeInteger(0, 1);
      return true;
    } else if (X >= -64 && X < 64) {
      if(!writer.canWriteBits(9)){
        throw new BufferFullException();
      }
      writer.writeBit(true);
      writer.writeBit(false);
      writer.writeInteger(X, 7);
      return true;
    } else if (X >= -256 && X < 256) {
      if(!writer.canWriteBits(12)){
        throw new BufferFullException();
      }
      writer.writeBit(true);
      writer.writeBit(true);
      writer.writeBit(false);
      writer.writeInteger(X, 9);
      return true;
    } else if (X >= -2048 && X < 2048) {
      if(!writer.canWriteBits(16)){
        throw new BufferFullException();
      }
      writer.writeBit(true);
      writer.writeBit(true);
      writer.writeBit(true);
      writer.writeBit(false);
      writer.writeInteger(X, 12);
      return true;
    } else {
      if(!writer.canWriteBits(36)){
        throw new BufferFullException();
      }
      writer.writeBit(true);
      writer.writeBit(true);
      writer.writeBit(true);
      writer.writeBit(true);
      writer.writeInteger(X, 32);
      return true;
    }
  }

  public static int readInteger(ValueReader reader) {
    var bit = reader.readBit();

    if (!bit) {
      return 0;
    } else {
      int read = 1;
      while (bit && read < 4) {
        bit = reader.readBit();
        read++;
      }

      if (read == 2) {
        return reader.readInteger(7);
      } else if (read == 3) {
        return reader.readInteger(9);
      } else {
        if (!bit) {
          return reader.readInteger(12);
        } else return reader.readInteger(32);
      }
    }
  }
}
