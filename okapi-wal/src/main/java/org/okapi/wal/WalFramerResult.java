package org.okapi.wal;

/** Result returned by a WalFramer after writing one record. */
public final class WalFramerResult {
    public final long lsn;
    public final long crc32c;
    public final long bytesWritten;

    public WalFramerResult(long lsn, long crc32c, long bytesWritten) {
        this.lsn = lsn;
        this.crc32c = crc32c;
        this.bytesWritten = bytesWritten;
    }
}
