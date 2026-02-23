/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.metrics.query;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Supplier;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class ParrallelAggregator<R> {

  ExecutorService service;

  public List<R> aggregate(List<Supplier<List<R>>> suppliers, Duration timeout)
      throws ExecutionException, InterruptedException, TimeoutException {
    var futures = new ArrayList<CompletableFuture<List<R>>>();
    for (var supplier : suppliers) {
      var future = CompletableFuture.supplyAsync(supplier, service);
      futures.add(future);
    }

    var all = new ArrayList<R>();
    for (var f : futures) {
      var result = f.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
      all.addAll(result);
    }
    return all;
  }
}
