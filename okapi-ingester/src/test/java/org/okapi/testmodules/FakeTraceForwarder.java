package org.okapi.testmodules;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import org.okapi.identity.Member;
import org.okapi.traces.io.ForwardedSpanRecord;
import org.okapi.traces.service.HttpTraceForwarder;

public class FakeTraceForwarder extends HttpTraceForwarder {
  public record Args(Member member, ForwardedSpanRecord record) {}

  @Getter private final List<Args> args = new ArrayList<>();

  public FakeTraceForwarder() {
    super(null, null);
  }

  @Override
  public void forward(Member member, ForwardedSpanRecord forwardedSpanRecord) {
    args.add(new Args(member, forwardedSpanRecord));
  }
}
