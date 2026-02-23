package org.okapi.wal;

import org.okapi.wal.lsn.Lsn;

public interface LsnSupplier {
    Lsn getLsn();
    Lsn next();
}
