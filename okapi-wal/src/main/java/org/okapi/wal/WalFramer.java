package org.okapi.wal;

import java.io.IOException;
import java.nio.channels.FileChannel;
import org.okapi.wal.Wal.WalRecord; // adjust if your generated package differs

/** Encodes a WalRecord to disk with an outer frame (e.g., LSN, CRC, length, payload). */
public interface WalFramer {

  /**
   * Conservative upper bound for per-record framing overhead in BYTES, including any
   * length-prefixes added by the framer and the 4-byte payload length prefix.
   */
  int perRecordOverheadBytes();

  /**
   * Write one framed record to the channel, returning the assigned LSN, CRC, and actual bytes
   * written. Must either fully write or throw.
   */
  WalFramerResult writeFramed(FileChannel channel, WalRecord record) throws IOException;
}
