package org.okapi.futures;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class OkapiFutures {
  public static <T> void fireAndForgetWait(Collection<Future<T>> futures) {
    for (var f : futures) {
      try {
        f.get();
      } catch (InterruptedException | ExecutionException e) {
        log.error("Execution failed.", e);
      }
    }
  }

  public static <T> List<Result<T, RuntimeException>> waitAndGetAll(List<Future<T>> suppliers) {
    var results = new ArrayList<Result<T, RuntimeException>>();
    for (var f : suppliers) {
      try {
        var val = f.get();
        results.add(Result.ofValue(val));
      } catch (ExecutionException | InterruptedException e) {
        results.add(Result.ofError(new RuntimeException(e)));
      }
    }
    return results;
  }
}
