/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.pages;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

public class MockPageBody {
  @Getter @Setter List<MockPageInput> inputs;

  public void append(MockPageInput pageInput) {
    inputs.add(pageInput);
  }

  public MockPageBody() {
    this.inputs = new ArrayList<>();
  }
}
