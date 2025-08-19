package org.okapi.wal;

import com.google.common.primitives.Ints;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.CRC32C;
import org.okapi.wal.Wal.Crc32;
import org.okapi.wal.Wal.Lsn;
import org.okapi.wal.Wal.WalRecord;

/**
 * WalFramer that requires the caller to supply an explicit LSN per write. Usage:
 * framer.setNextLsn(lsnFromShardMap); writer.write(walRecord); // will consume the set LSN
 */
public final class ManualLsnWalFramer implements WalFramer {
  private final int overheadBytes;
  private final AtomicReference<Long> nextLsnRef = new AtomicReference<>();

  public ManualLsnWalFramer(int overheadBytes) {
    this.overheadBytes = overheadBytes;
  }

  @Override
  public int perRecordOverheadBytes() {
    return overheadBytes;
  }

  /** Set the exact LSN to be used on the next write. Must be set before writeFramed. */
  public void setNextLsn(long lsn) {
    nextLsnRef.set(lsn);
  }

  @Override
  public WalFramerResult writeFramed(FileChannel channel, WalRecord record) throws IOException {
    Long lsn = nextLsnRef.getAndSet(null);
    if (lsn == null) {
      throw new IllegalStateException("ManualLsnWalFramer: next LSN not set before write");
    }

    // Build payload bytes
    byte[] payload = record.toByteArray();

    // Compute CRC32C on payload
    CRC32C crc32c = new CRC32C();
    crc32c.update(payload, 0, payload.length);
    long crcValue = Integer.toUnsignedLong((int) crc32c.getValue());

    // Build tuple parts
    byte[] lsnBytes = Lsn.newBuilder().setN(lsn).build().toByteArray();
    byte[] crcBytes = Crc32.newBuilder().setCrc(crcValue).build().toByteArray();

    long before = channel.position();

    // [len][LSN]
    writeLenPrefixed(channel, lsnBytes);
    // [len][CRC]
    writeLenPrefixed(channel, crcBytes);
    // [len][payload]
    writeLenPrefixed(channel, payload);

    long after = channel.position();
    long bytesWritten = after - before;

    return new WalFramerResult(lsn, crcValue, bytesWritten);
  }

  private static void writeLenPrefixed(FileChannel ch, byte[] data) throws IOException {
    ByteBuffer len = ByteBuffer.wrap(Ints.toByteArray(data.length));
    while (len.hasRemaining()) ch.write(len);
    ByteBuffer body = ByteBuffer.wrap(data);
    while (body.hasRemaining()) ch.write(body);
  }
}
