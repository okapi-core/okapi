package org.okapi.logs.ch;

import io.opentelemetry.proto.collector.logs.v1.ExportLogsServiceRequest;
import io.opentelemetry.proto.collector.logs.v1.ExportLogsServiceResponse;

public class ChLogsIngester {
  public ExportLogsServiceResponse ingest(ExportLogsServiceRequest serviceRequest) {
    return ExportLogsServiceResponse.newBuilder().build();
  }
}
