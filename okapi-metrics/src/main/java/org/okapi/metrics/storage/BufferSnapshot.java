/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.metrics.storage;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;

public interface BufferSnapshot<T> extends Iterator<T> {
  void write(OutputStream os) throws IOException;
}
