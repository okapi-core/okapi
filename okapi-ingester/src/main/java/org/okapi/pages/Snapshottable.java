package org.okapi.pages;

import java.util.List;

public interface Snapshottable<Record> {
  List<Record> snapshot();
}
