package org.okapi.metrics.otel;

import java.util.Locale;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class ConversionConfig {
  private final boolean prometheusDialect;

  public static ConversionConfig fromHeader(String headerValue) {
    if (headerValue == null || headerValue.isBlank()) {
      return new ConversionConfig(false);
    }
    var normalized = headerValue.trim().toLowerCase(Locale.ROOT);
    return new ConversionConfig("prometheus".equals(normalized));
  }
}
