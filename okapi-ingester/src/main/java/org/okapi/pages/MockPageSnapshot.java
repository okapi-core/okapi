package org.okapi.pages;

import lombok.AllArgsConstructor;

import java.util.List;

@AllArgsConstructor
public class MockPageSnapshot {
  List<MockPageInput> inputs;
  long tsStart;
  long tsEnd;
}
