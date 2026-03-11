package org.okapi.oscar.tools;

import java.time.Instant;
import java.util.concurrent.TimeUnit;
import lombok.Builder;
import lombok.Getter;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
public class DateTimeTools {

  @Tool(description = "Returns the current time as milliseconds since Unix epoch.")
  public long currentTime() {
    return System.currentTimeMillis();
  }

  @Tool(description = "Returns the current time as nanoseconds since Unix epoch.")
  public long currentTimeNanos() {
    Instant now = Instant.now();
    return now.getEpochSecond() * 1_000_000_000L + now.getNano();
  }

  @Tool(
      description =
          "Returns a time range [startMs, endMs] ending at now and spanning the given duration."
              + " Use this to compute query windows like 'last 1 hour' or 'last 30 minutes'.")
  public TimeRange timeRange(
      @ToolParam(description = "Duration value, e.g. 1 for 1 hour or 30 for 30 minutes.")
          int duration,
      @ToolParam(
              description =
                  "Time unit for the duration. Valid values: SECONDS, MINUTES, HOURS, DAYS.")
          TimeUnit unit) {
    long endMs = System.currentTimeMillis();
    long startMs = endMs - unit.toMillis(duration);
    return TimeRange.builder().startMs(startMs).endMs(endMs).build();
  }

  @Tool(
      description =
          "Returns a time range [startNanos, endNanos] ending at now and spanning the given"
              + " duration, expressed in nanoseconds since Unix epoch. Use this for trace queries"
              + " which require nanosecond timestamps.")
  public TimeRangeNanos timeRangeNanos(
      @ToolParam(description = "Duration value, e.g. 1 for 1 hour or 30 for 30 minutes.")
          int duration,
      @ToolParam(
              description =
                  "Time unit for the duration. Valid values: SECONDS, MINUTES, HOURS, DAYS.")
          TimeUnit unit) {
    Instant now = Instant.now();
    long endNanos = now.getEpochSecond() * 1_000_000_000L + now.getNano();
    long startNanos = endNanos - unit.toNanos(duration);
    return TimeRangeNanos.builder().startNanos(startNanos).endNanos(endNanos).build();
  }

  @Builder
  @Getter
  public static class TimeRange {
    long startMs;
    long endMs;
  }

  @Builder
  @Getter
  public static class TimeRangeNanos {
    long startNanos;
    long endNanos;
  }
}
