package org.okapi.abstractio;

public interface TimeRangeSnapshot {

  long getTsStart();

  long getTsEnd();

  int getNDocs();
}
