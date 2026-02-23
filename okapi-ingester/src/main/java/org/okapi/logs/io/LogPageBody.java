package org.okapi.logs.io;

import java.util.List;
import org.okapi.pages.AbstractListBackedPageBody;
import org.okapi.primitives.BinaryLogRecordV1;

public class LogPageBody extends AbstractListBackedPageBody<BinaryLogRecordV1> {

  public LogPageBody() {
    super();
  }

  protected LogPageBody(List<BinaryLogRecordV1> logDocs) {
    super(logDocs);
  }

  public LogBodySnapshot toSnapshot() {
    return new LogBodySnapshot(super.snapshot());
  }
}
