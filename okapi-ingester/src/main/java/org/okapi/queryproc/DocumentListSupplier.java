package org.okapi.queryproc;

import java.util.List;

public interface DocumentListSupplier<T extends IdentifiableDocument> {

  List<T> getDocuments() throws Exception;
}
