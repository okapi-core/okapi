package org.okapi.ds;

import java.io.Closeable;
import java.io.IOException;
import java.util.Set;

public interface PersistedSet<T> extends Closeable {
    void add(T el) throws IOException;
    void remove(T el) throws IOException;
    boolean contains(T el);
    void flush() throws IOException;
    Set<T> list();
}
