package org.okapi.pages;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
@AllArgsConstructor
public class MockPageInput {
  long tsMillis;
  String content;
  int size;
}
