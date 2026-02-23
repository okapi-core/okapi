package org.okapi.logs.forwarding;

import org.okapi.identity.Member;
import org.okapi.logs.io.ForwardedLogIngestRecord;

public interface LogForwarder {
  void forward(Member member, ForwardedLogIngestRecord records);
}
