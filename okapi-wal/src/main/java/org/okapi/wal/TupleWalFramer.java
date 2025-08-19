package org.okapi.wal;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.primitives.Ints;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.function.LongSupplier;
import java.util.zip.CRC32C;
import org.okapi.metrics.io.OkapiIo;
import org.okapi.wal.Wal.Crc32;
import org.okapi.wal.Wal.Lsn;
import org.okapi.wal.Wal.WalRecord;

/** Framing as a tuple: [len][LSN], [len][CRC], [len][WalRecordPayload] */
public final class TupleWalFramer implements WalFramer {

  private final LongSupplier lsnSupplier;
  private final int varintLenEstimate; // 1..10

  public TupleWalFramer(LongSupplier lsnSupplier, int varintLenEstimate) {
    this.lsnSupplier = checkNotNull(lsnSupplier, "lsnSupplier");
    checkArgument(
        varintLenEstimate >= 1 && varintLenEstimate <= 10, "varintLenEstimate must be 1..10");
    this.varintLenEstimate = varintLenEstimate;
  }

  @Override
  public int perRecordOverheadBytes() {
    // [len+lsnProto] + [len+crcProto] + [len for payload]
    int lsnProto = 4 + (1 + varintLenEstimate); // tag(1) + varint(n)
    int crcProto = 4 + (1 + varintLenEstimate);
    return lsnProto + crcProto + 4; // +4 for payload length prefix (OkapiIo)
  }

  @Override
  public WalFramerResult writeFramed(FileChannel channel, WalRecord record) throws IOException {
    // Serialize payload once
    byte[] payload = record.toByteArray();

    // Compute CRC32C over the payload bytes
    CRC32C crc32c = new CRC32C();
    crc32c.update(payload, 0, payload.length);
    long crcValue = Integer.toUnsignedLong((int) crc32c.getValue());

    // Assign LSN
    long lsnValue = lsnSupplier.getAsLong();

    // Build small protos
    Lsn lsnMsg = Lsn.newBuilder().setN(lsnValue).build();
    Crc32 crcMsg = Crc32.newBuilder().setCrc(crcValue).build();

    long written = 0;
    written += writePrefixAndBytes(channel, lsnMsg.toByteArray());
    written += writePrefixAndBytes(channel, crcMsg.toByteArray());

    OkapiIo.write(channel, payload); // [len][payload]
    written += 4L + payload.length;

    return new WalFramerResult(lsnValue, crcValue, written);
  }

  private static long writePrefixAndBytes(FileChannel channel, byte[] bytes) throws IOException {
    ByteBuffer header = ByteBuffer.wrap(Ints.toByteArray(bytes.length));
    while (header.hasRemaining()) channel.write(header);
    ByteBuffer body = ByteBuffer.wrap(bytes);
    while (body.hasRemaining()) channel.write(body);
    return 4L + bytes.length;
  }
}
