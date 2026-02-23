package org.okapi.demo.rest;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class WordCount {
  String sentenceId;
  int count;
}
