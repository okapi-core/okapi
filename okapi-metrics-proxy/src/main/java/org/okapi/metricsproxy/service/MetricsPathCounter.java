package org.okapi.metricsproxy.service;

import org.okapi.hashing.OkapiHashingUtils;
import com.okapi.rest.metrics.MetricsPathSpecifier;
import lombok.Getter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MetricsPathCounter {
  Set<Integer> inBucket = new HashSet<>();
  @Getter List<MetricsPathSpecifier> paths = new ArrayList<>();

  public void add(MetricsPathSpecifier pathSpecifier) {
    var hash = OkapiHashingUtils.computeHash(pathSpecifier.getName(), pathSpecifier.getTags());
    if (inBucket.contains(hash)) {
      return;
    } else {
      inBucket.add(hash);
      paths.add(pathSpecifier);
    }
  }

  public void addAll(List<MetricsPathSpecifier> paths) {
    for (var p : paths) {
      add(p);
    }
  }
}
