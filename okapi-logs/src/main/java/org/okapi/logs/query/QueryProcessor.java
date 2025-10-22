package org.okapi.logs.query;

import java.io.IOException;
import java.util.List;
import org.okapi.protos.logs.LogPayloadProto;

public interface QueryProcessor {
  List<LogPayloadProto> getLogs(
      String tenantId,
      String logStream,
      long start,
      long end,
      LogFilter filter,
      QueryConfig cfg)
      throws IOException;
}
