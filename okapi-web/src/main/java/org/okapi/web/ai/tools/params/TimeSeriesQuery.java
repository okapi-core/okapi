package org.okapi.web.ai.tools.params;

import java.util.SortedMap;
import lombok.Getter;

@Getter
public class TimeSeriesQuery {
  // metric path name
  String pathName;
  // metric tags
  SortedMap<String, String> tags;
  // start time in milliseconds: linux epoch
  long startTime;
  // end time in milliseconds: linux epoch
  long endTime;
  // resolution in milliseconds: this is optional, not necessary to honor
  Long resolution;

  public TimeSeriesQuery(
      String pathName,
      SortedMap<String, String> tags,
      long startTime,
      long endTime,
      Long resolution) {
    this.pathName = pathName;
    this.tags = tags;
    this.startTime = startTime;
    this.endTime = endTime;
    this.resolution = resolution;
  }
}
