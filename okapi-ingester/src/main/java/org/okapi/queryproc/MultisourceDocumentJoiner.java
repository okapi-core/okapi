/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.queryproc;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor
public class MultisourceDocumentJoiner<T extends IdentifiableDocument> {

  List<DocumentListSupplier<T>> suppliers;
  ExecutorService executorService;

  public List<T> getJoinedStream(Duration perQueryTimeout) {
    var futures = new ArrayList<CompletableFuture<List<T>>>();
    var seen = new ArrayList<String>();
    for (var supplier : suppliers) {
      futures.add(
          CompletableFuture.supplyAsync(
              () -> {
                try {
                  return supplier.getDocuments();
                } catch (Exception e) {
                  throw new RuntimeException(e);
                }
              },
              executorService));
    }
    var allResults = new ArrayList<T>();
    for (var future : futures) {
      List<T> results = future.orTimeout(perQueryTimeout.toMillis(), TimeUnit.MILLISECONDS).join();
      for (var doc : results) {
        if (!seen.contains(doc.getDocId())) {
          seen.add(doc.getDocId());
          allResults.add(doc);
        }
      }
    }
    return allResults;
  }
}
