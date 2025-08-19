package org.okapi.wal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.nio.file.Path;

public interface WalStreamer {
  @AllArgsConstructor
  @Builder
  @NoArgsConstructor
  public static final class Options {
    /** If true, run STRICT_TRUNCATE recovery once before streaming. Default true. */
    public boolean runRecovery = true;

    /** If true, verify CRC32C(payload) matches CRC from the tuple. Default true. */
    public boolean verifyCrc = true;

    /**
     * If true, fence to the durable watermark by reading persisted.lsn and never streaming beyond
     * that LSN. Default false (stream all bytes present at fence time).
     */
    public boolean fenceToPersistedLsn = false;

    /**
     * Optional explicit upper LSN bound. If set, streaming will not go past this LSN even if more
     * bytes exist in files. Takes precedence over fenceToPersistedLsn.
     */
    public Long upToLsn = null;
  }

  @AllArgsConstructor
  public static final class Result {
    /** Number of records delivered to the consumer during this run. */
    public final long recordsDelivered;

    /**
     * The last LSN delivered (== consumer watermark on success), or the starting watermark if none
     * delivered.
     */
    public final long lastDeliveredLsn;

    /** Number of segments visited during this run. */
    public final int segmentsVisited;
  }

  Result stream(Path root, WalStreamConsumer consumer, Options opts) throws IOException;
}
