package org.okapi.logs.forwarding;

import io.opentelemetry.proto.logs.v1.LogRecord;
import java.util.List;
import org.okapi.swim.ping.Member;

public interface LogForwarder {
  void forward(String tenantId, String logStream, Member member, List<LogRecord> records);
}

