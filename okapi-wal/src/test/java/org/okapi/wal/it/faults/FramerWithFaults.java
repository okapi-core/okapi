package org.okapi.wal.it.faults;

import com.google.common.primitives.Ints;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.CRC32C;
import org.okapi.metrics.io.OkapiIo;
import org.okapi.wal.*;
import org.okapi.wal.Wal.Crc32;
import org.okapi.wal.Wal.Lsn;
import org.okapi.wal.Wal.WalRecord;

/** A framer for tests that injects crashes at specific phases. */
public final class FramerWithFaults implements WalFramer {
  private final int overhead;
  private final AtomicLong lsnCounter;
  private final CrashInjector injector;

  public FramerWithFaults(int overhead, AtomicLong lsnCounter, CrashInjector injector) {
    this.overhead = overhead;
    this.lsnCounter = lsnCounter;
    this.injector = injector;
  }

  @Override
  public int perRecordOverheadBytes() {
    return overhead;
  }

  @Override
  public WalFramerResult writeFramed(FileChannel channel, WalRecord record) throws IOException {
    injector.hit(CrashPoint.BEFORE_LSN);

    long lsn = lsnCounter.incrementAndGet();
    Lsn lsnMsg = Lsn.newBuilder().setN(lsn).build();
    byte[] lsnBytes = lsnMsg.toByteArray();

    // LSN
    writePrefixAndBytes(channel, lsnBytes);
    injector.hit(CrashPoint.AFTER_LSN_BEFORE_CRC);

    // CRC
    byte[] payload = record.toByteArray();
    CRC32C crc = new CRC32C();
    crc.update(payload, 0, payload.length);
    long crcValue = Integer.toUnsignedLong((int) crc.getValue());
    Crc32 crcMsg = Crc32.newBuilder().setCrc(crcValue).build();
    byte[] crcBytes = crcMsg.toByteArray();

    writePrefixAndBytes(channel, crcBytes);
    injector.hit(CrashPoint.AFTER_CRC_BEFORE_PAYLOAD);

    // PAYLOAD
    injector.hit(CrashPoint.DURING_PAYLOAD);
    OkapiIo.write(channel, payload);

    long bytesWritten = 4L + lsnBytes.length + 4L + crcBytes.length + 4L + payload.length;
    return new WalFramerResult(lsn, crcValue, bytesWritten);
  }

  private static void writePrefixAndBytes(FileChannel channel, byte[] bytes) throws IOException {
    ByteBuffer header = ByteBuffer.wrap(Ints.toByteArray(bytes.length));
    while (header.hasRemaining()) channel.write(header);
    ByteBuffer body = ByteBuffer.wrap(bytes);
    while (body.hasRemaining()) channel.write(body);
  }
}
