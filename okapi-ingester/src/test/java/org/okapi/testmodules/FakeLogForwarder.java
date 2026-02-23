/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.testmodules;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import org.okapi.identity.Member;
import org.okapi.logs.forwarding.LogForwarder;
import org.okapi.logs.io.ForwardedLogIngestRecord;

public class FakeLogForwarder implements LogForwarder {
  @Override
  public void forward(Member member, ForwardedLogIngestRecord records) {
    this.args.add(new Args(member, records));
  }

  public record Args(Member member, ForwardedLogIngestRecord records) {}

  @Getter private final List<Args> args = new ArrayList<>();
}
