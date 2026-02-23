/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.queryproc;

import java.util.List;

public interface DocumentListSupplier<T extends IdentifiableDocument> {

  List<T> getDocuments() throws Exception;
}
