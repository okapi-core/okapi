package org.okapi.web.service.federation;

import org.okapi.web.tsvector.LegendFn;

public interface LegendParser<T> {
  LegendFn<T> createLegend(String def);
}
