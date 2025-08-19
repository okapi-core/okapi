package org.okapi.wal;

import com.google.common.primitives.Ints;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.CRC32C;
import org.okapi.metrics.io.OkapiIo;
import org.okapi.wal.Wal.WalRecord;

/**
 * Test framer: - perRecordOverheadBytes: configurable constant (conservative). - writeFramed:
 * writes [len][LSN-bytes], [len][CRC-bytes], [len][payload] realistically. - LSNs generated from
 * provided AtomicLong (increment then use).
 */
public class FakeFramer implements WalFramer {

  private final int overhead;
  private final AtomicLong lsnCounter;
  private boolean throwOnPayload = false;

  public FakeFramer(int overhead) {
    this(overhead, new AtomicLong(0));
  }

  public FakeFramer(int overhead, AtomicLong lsn) {
    this.overhead = overhead;
    this.lsnCounter = lsn;
  }

  public void setThrowOnPayload(boolean v) {
    this.throwOnPayload = v;
  }

  @Override
  public int perRecordOverheadBytes() {
    return overhead;
  }

  @Override
  public WalFramerResult writeFramed(FileChannel channel, WalRecord record) throws IOException {
    long lsn = lsnCounter.incrementAndGet();
    byte[] payload = record.toByteArray();

    // Compute CRC32C over payload
    CRC32C crc = new CRC32C();
    crc.update(payload, 0, payload.length);
    long crcValue = Integer.toUnsignedLong((int) crc.getValue());

    long written = 0;

    byte[] lsnBytes = longToBytes(lsn);
    written += writePrefixAndBytes(channel, lsnBytes);

    byte[] crcBytes = longToBytes(crcValue);
    written += writePrefixAndBytes(channel, crcBytes);

    if (throwOnPayload) {
      throw new RuntimeException("Injected failure during payload write");
    }

    OkapiIo.write(channel, payload);
    written += 4L + payload.length;

    return new WalFramerResult(lsn, crcValue, written);
  }

  private static long writePrefixAndBytes(FileChannel channel, byte[] bytes) throws IOException {
    ByteBuffer header = ByteBuffer.wrap(Ints.toByteArray(bytes.length));
    while (header.hasRemaining()) channel.write(header);
    ByteBuffer body = ByteBuffer.wrap(bytes);
    while (body.hasRemaining()) channel.write(body);
    return 4L + bytes.length;
  }

  private static byte[] longToBytes(long v) {
    // 8 bytes big-endian for simplicity
    return ByteBuffer.allocate(8).putLong(0, v).array();
  }
}
