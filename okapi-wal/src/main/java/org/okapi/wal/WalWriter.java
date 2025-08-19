package org.okapi.wal;

import com.google.protobuf.GeneratedMessageV3;
import java.io.Closeable;
import java.io.IOException;

public interface WalWriter<T extends GeneratedMessageV3> extends Closeable {
    void write(T record) throws IOException;

    @Override
    default void close() throws IOException {}
}
