package org.okapi.metrics.service.web;

import static org.okapi.validation.OkapiChecks.checkArgument;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import lombok.AllArgsConstructor;
import org.okapi.exceptions.BadRequestException;
import org.okapi.metrics.PathRegistry;
import org.okapi.metrics.ShardMap;
import org.okapi.metrics.common.MetricPaths;
import org.okapi.metrics.common.ServiceRegistry;
import org.okapi.metrics.common.sharding.ShardsAndSeriesAssignerFactory;
import org.okapi.metrics.paths.PathSet;
import org.okapi.metrics.query.QueryRecords;
import org.okapi.metrics.rocks.RocksStore;
import org.okapi.metrics.rollup.HashFns;
import org.okapi.metrics.rollup.RocksReaderSupplier;
import org.okapi.metrics.rollup.RollupQueryProcessor;
import org.okapi.metrics.search.MetricsSearcher;
import org.okapi.rest.metrics.*;
import org.rocksdb.RocksDBException;

@AllArgsConstructor
public class RocksQueryProcessor implements QueryProcessor {
  ShardMap shardMap;
  ShardsAndSeriesAssignerFactory shardsAndSeriesAssignerFactory;
  RollupQueryProcessor rollupQueryProcessor;
  ServiceRegistry serviceRegistry;
  RocksReaderSupplier rocksReaderSupplier;
  PathSet pathSet;
  RocksStore rocksStore;
  PathRegistry pathRegistry;
  

  public GetMetricsResponse getMetricsResponse(GetMetricsRequestInternal request) throws Exception {
    var nodes = serviceRegistry.listActiveNodes();
    if (nodes.isEmpty()) {
      throw new BadRequestException("Cluster is scaling up, shard position unknown.");
    }

    var assigner = shardsAndSeriesAssignerFactory.makeAssigner(shardMap.getNsh(), nodes.get());
    var path = MetricPaths.convertToPath(request);
    var shard = assigner.getShard(path);
    var rocksReader = rocksReaderSupplier.apply(shard);
    if (rocksReader.isEmpty()) {
      return GetMetricsResponse.builder()
          .name(request.getMetricName())
          .resolution(request.getResolution())
          .aggregation(request.getAggregation())
          .tenant(request.getTenantId())
          .tags(request.getTags())
          .values(Collections.emptyList())
          .times(Collections.emptyList())
          .build();
    }
    var result =
        rollupQueryProcessor.scan(
            rocksReader.get(),
            new QueryRecords.Slice(
                path,
                request.getStart(),
                request.getEnd(),
                request.getResolution(),
                request.getAggregation()));
    return GetMetricsResponse.builder()
        .name(request.getMetricName())
        .resolution(request.getResolution())
        .aggregation(request.getAggregation())
        .tenant(request.getTenantId())
        .tags(request.getTags())
        .values(result.values())
        .times(result.timestamps())
        .build();
  }

  public SearchMetricsResponse searchMetricsResponse(
      SearchMetricsRequestInternal searchMetricsRequest)
      throws BadRequestException, RocksDBException, IOException {
    checkArgument(searchMetricsRequest.getStartTime() > 0, BadRequestException::new);
    checkArgument(searchMetricsRequest.getEndTime() > 0, BadRequestException::new);
    var candidates = pathSet.list();
    var results = new ArrayList<MetricsPathSpecifier>();
    for (var entry : candidates.entrySet()) {
      var shard = entry.getKey();
      var paths = entry.getValue();
      var matching =
          new HashSet<>(
              MetricsSearcher.searchMatchingMetrics(
                  searchMetricsRequest.getTenantId(), paths, searchMetricsRequest.getPattern()));
      var reader = rocksStore.rocksReader(pathRegistry.rocksPath(shard));
      if (reader.isEmpty()) continue;
      var discretizer = 3600_000;
      var hrStart = discretizer * (searchMetricsRequest.getStartTime() / discretizer);
      var hrEnd = discretizer * (searchMetricsRequest.getEndTime() / discretizer);

      for (var match : matching) {
        var lookups = new ArrayList<byte[]>();
        for (var hr = hrStart; hr <= hrEnd; hr += discretizer) {
          var path = MetricPaths.convertToPath(match.tenantId(), match.name(), match.tags());
          var lookUpKey = HashFns.hourlyBucket(path, hr);
          lookups.add(lookUpKey.getBytes());
        }
        var hasAMatch = reader.get().getBatch(lookups).stream().anyMatch(Objects::nonNull);
        if (hasAMatch) {
          results.add(new MetricsPathSpecifier(match.name(), match.tags()));
        }
      }
    }

    return SearchMetricsResponse.builder().results(results).build();
  }
}
