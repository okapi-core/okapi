package org.okapi.metrics.io;

import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class OkapiIo {

  public static int readInt(InputStream is) throws IOException, StreamReadingException {
    // read a 32 bit integer from the stream
    byte[] intBytes = new byte[4];
    for (int i = 0; i < 4; i++) {
      int read = is.read();
      if (read == -1) {
        throw new StreamReadingException("Not enough bytes to read an integer");
      }
      intBytes[i] = (byte) read;
    }

    // convert the byte array to an integer
    return Ints.fromByteArray(intBytes);
  }

  public static long readLong(InputStream is) throws IOException, StreamReadingException {
    byte[] longBytes = new byte[8];
    for (int i = 0; i < 8; i++) {
      int read = is.read();
      if (read == -1) {
        throw new StreamReadingException("Not enough bytes to read an integer");
      }
      longBytes[i] = (byte) read;
    }
    return Longs.fromByteArray(longBytes);
  }

  public static int writeInt(OutputStream os, int x) throws IOException {
    var bytes = Ints.toByteArray(x);
    os.write(bytes);
    return bytes.length;
  }

  public static int writeLong(OutputStream os, long t) throws IOException {
    var arr = Longs.toByteArray(t);
    os.write(arr);
    return arr.length;
  }

  public static int writeBytes(OutputStream os, byte[] bytes) throws IOException {
    var written = writeInt(os, bytes.length); // write the length of the byte array
    os.write(bytes); // write the byte array
    written += bytes.length;
    return written;
  }

  public static int writeString(OutputStream os, String s) throws IOException {
    return writeBytes(os, s.getBytes());
  }

  public static String readString(InputStream is) throws IOException, StreamReadingException {
    byte[] bytes = OkapiIo.readBytes(is);
    return new String(bytes);
  }

  public static byte[] readBytes(InputStream is) throws IOException, StreamReadingException {
    int length = readInt(is);
    byte[] bytes = new byte[length];
    int read = is.read(bytes);
    if (read != length) {
      throw new StreamReadingException("Not enough bytes to read the byte array");
    }
    return bytes;
  }

  public static void write(OutputStream os, byte b) throws IOException {
    os.write(b);
  }

  public static byte read(InputStream is) throws IOException, StreamReadingException {
    int read = is.read();
    if (read == -1) {
      throw new StreamReadingException("Not enough bytes to read a byte");
    }
    return (byte) read;
  }

  public static void checkMagicNumber(InputStream is, String expectedMagicNumber)
      throws IOException, StreamReadingException {
    var inputStr = OkapiIo.readString(is);
    if (!inputStr.equals(expectedMagicNumber)) {
      throw new StreamReadingException(
          "Invalid magic number. Expected: " + expectedMagicNumber + ", but got: " + inputStr);
    }
  }

  public static void write(FileChannel channel, byte[] payload) throws IOException {
    byte[] header = Ints.toByteArray(payload.length);
    ByteBuffer buf = ByteBuffer.allocate(4 + payload.length);
    buf.put(header);
    buf.put(payload);
    buf.flip();
    while (buf.hasRemaining()) {
      channel.write(buf);
    }
  }
}
