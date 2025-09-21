package org.okapi.metrics.cas;

import java.util.Map;
import java.util.TreeMap;
import org.okapi.metrics.cas.dao.TypeHintsDao;
import org.okapi.metrics.cas.dto.TypeHints;
import org.okapi.metrics.common.MetricPaths;
import org.okapi.metrics.fdb.MetricTypesSearchVals;
import org.okapi.metrics.pojos.AGG_TYPE;
import org.okapi.metrics.pojos.RES_TYPE;
import org.okapi.metrics.pojos.SUM_TYPE;
import org.okapi.metrics.pojos.results.Scan;
import org.okapi.metrics.rollup.TsReader;
import org.okapi.promql.eval.ts.RESOLUTION;
import org.okapi.promql.eval.ts.TsClient;

public class CasTsClient implements TsClient {
  String tenantId;
  TsReader reader;
  TypeHintsDao typeHintsDao;

  @Override
  public Scan get(String name, Map<String, String> unsortedTags, RESOLUTION res, long startMs, long endMs) {
    var sortedCopy = new TreeMap<>(unsortedTags);
    String localPath = MetricPaths.localPath(name, sortedCopy);
    TypeHints hint = typeHintsDao.get(tenantId, localPath);
    String metricType = (hint != null) ? hint.getMetricType() : null;
    String series = MetricPaths.univPath(tenantId, name, sortedCopy);
    RES_TYPE resolution =
        switch (res) {
          case SECONDLY -> RES_TYPE.SECONDLY;
          case MINUTELY -> RES_TYPE.MINUTELY;
          case HOURLY -> RES_TYPE.HOURLY;
        };

    if (MetricTypesSearchVals.GAUGE.equals(metricType)) {
      return reader.scanGauge(series, startMs, endMs, AGG_TYPE.AVG, resolution);
    } else if (MetricTypesSearchVals.HISTO.equals(metricType)) {
      return reader.scanHisto(series, startMs, endMs);
    } else if (MetricTypesSearchVals.SUM.equals(metricType)) {
      long windowSize = Math.max(endMs - startMs, 1L);
      return reader.scanSum(series, startMs, endMs, windowSize, SUM_TYPE.DELTA);
    } else if (MetricTypesSearchVals.CSUM.equals(metricType)) {
      long windowSize = Math.max(endMs - startMs, 1L);
      return reader.scanSum(series, startMs, endMs, windowSize, SUM_TYPE.CSUM);
    }

    // Fallback: no hint found; default to gauge scan
    return reader.scanGauge(series, startMs, endMs, AGG_TYPE.AVG, resolution);
  }
}
