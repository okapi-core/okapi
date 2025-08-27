package org.okapi.metrics.paths;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;
import lombok.extern.slf4j.Slf4j;
import org.okapi.constants.Constants;
import org.okapi.ds.FileBackedSet;

@Slf4j
public class PathSet {
  private final PersistedSetWalPathSupplier walPathSupplier;
  private final Map<Integer, FileBackedSet> fileBackedSetMap;
  private final Multimap<String, Integer> reverse;

  public PathSet(PersistedSetWalPathSupplier walPathSupplier) {
    this.fileBackedSetMap = new ConcurrentHashMap<>();
    this.reverse = ArrayListMultimap.create();
    IntStream.range(0, Constants.N_SHARDS)
        .forEach(
            shard -> {
              var path = walPathSupplier.apply(shard);
              if (!Files.exists(path)) return;
              try {
                fileBackedSetMap.put(shard, new FileBackedSet(path));
              } catch (IOException e) {
                throw new RuntimeException(e);
              }
            });
    for (var entry : fileBackedSetMap.entrySet()) {
      var key = entry.getKey();
      var paths = entry.getValue().list();
      for (var path : paths) {
        reverse.put(path, key);
      }
    }
    this.walPathSupplier = walPathSupplier;
  }

  public FileBackedSet get(Integer shard) {
    var set =
        this.fileBackedSetMap.computeIfAbsent(
            shard,
            (sh) -> {
              var path = walPathSupplier.apply(sh);
              try {
                return new FileBackedSet(path);
              } catch (IOException e) {
                throw new RuntimeException(e);
              }
            });
    return set;
  }

  public void add(Integer shard, String metricPath) throws IOException {
    get(shard).add(metricPath);
    reverse.put(metricPath, shard);
  }

  public void delete(Integer shard, String metricPath) throws IOException {
    get(shard).remove(metricPath);
    reverse.remove(metricPath, shard);
  }

  public Map<Integer, Set<String>> list() {
    var paths = new HashMap<Integer, Set<String>>();
    for (var shard : this.fileBackedSetMap.keySet()) {
      var set = paths.computeIfAbsent(shard, sh -> new HashSet<>());
      set.addAll(get(shard).list());
    }
    return paths;
  }

  public Set<String> pathsInShard(int shard) {
    return this.fileBackedSetMap.get(shard).list();
  }

  public Collection<Integer> shardsForPath(String path) {
    return reverse.get(path);
  }

  public void sync() throws IOException {
    for (var shard : this.fileBackedSetMap.entrySet()) {
      shard.getValue().flush();
    }
  }
}
