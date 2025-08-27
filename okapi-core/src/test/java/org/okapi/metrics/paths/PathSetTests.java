package org.okapi.metrics.paths;

import static org.junit.jupiter.api.Assertions.*;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class PathSetTests {

  @TempDir Path walRoot;

  Path wal;
  PersistedSetWalPathSupplier pathSupplier;
  PathSet pathSet;

  @BeforeEach
  public void setup() {
    wal = walRoot.resolve("pathSet.wal");
    pathSupplier =
        new PersistedSetWalPathSupplier() {
          @Override
          public Path apply(Integer shard) {
            return walRoot.resolve(Integer.toString(shard));
          }
        };
    pathSet = new PathSet(pathSupplier);
  }

  @Test
  public void testAddPaths() throws IOException {
    pathSet.add(0, "path{}");
    var list = pathSet.list();
    assertTrue(list.containsKey(0));
    assertEquals(Sets.newHashSet("path{}"), list.get(0));
    var reverse = pathSet.shardsForPath("path{}");
    assertTrue(reverse.contains(0));
  }

  @Test
  public void testAddDeletePaths() throws IOException {
    pathSet.add(0, "path{}");
    pathSet.delete(0, "path{}");
    var list = pathSet.list();
    assertTrue(list.containsKey(0));
    assertTrue(list.get(0).isEmpty());
    var reverse = pathSet.shardsForPath("path{}");
    assertFalse(reverse.contains(0));
    assertTrue(reverse.isEmpty());
  }

  @Test
  public void testPathSetReloadsFromDisk() throws IOException {
    pathSet.add(0, "path{}");
    var reloaded = new PathSet(pathSupplier);
    var list = reloaded.list();
    assertTrue(list.containsKey(0));
    assertFalse(list.get(0).isEmpty());

    var reverse = pathSet.shardsForPath("path{}");
    assertTrue(reverse.contains(0));
  }

  @ParameterizedTest
  @MethodSource("testMultiplePathsArgs")
  public void testMultiplePaths(Map<Integer, Collection<String>> paths) throws IOException {
    for (var entry : paths.entrySet()) {
      entry
          .getValue()
          .forEach(
              c -> {
                try {
                  pathSet.add(entry.getKey(), c);
                } catch (IOException e) {
                  throw new RuntimeException(e);
                }
              });
    }

    assertPathSetMatch(pathSet, paths);

    // check reload
    pathSet.sync();
    var reload = new PathSet(pathSupplier);

    assertPathSetMatch(reload, paths);
  }

  private void assertPathSetMatch(PathSet pathSet, Map<Integer, Collection<String>> reference) {
    Multimap<Integer, String> shardToPath = ArrayListMultimap.create();
    Multimap<String, Integer> inverse = ArrayListMultimap.create();

    for (var entry : reference.entrySet()) {
      entry
          .getValue()
          .forEach(
              c -> {
                shardToPath.put(entry.getKey(), c);
                inverse.put(c, entry.getKey());
              });
    }

    var list = pathSet.list();
    assertEquals(list, reference);

    // check direct maps
    for (var entry : reference.entrySet()) {
      assertEquals(entry.getValue(), pathSet.pathsInShard(entry.getKey()));
    }

    // check inversions
    for (var entry : reference.entrySet()) {
      for (var val : entry.getValue()) {
        var shards = pathSet.shardsForPath(val);
        assertEquals(inverse.get(val), shards);
      }
    }
  }

  public static Stream<Arguments> testMultiplePathsArgs() {
    return Stream.of(
        Arguments.of(Map.of(0, Sets.newHashSet("path{}"))),
        Arguments.of(Map.of(0, Sets.newHashSet("path{}", "path-2{}"))),
        Arguments.of(Map.of(0, Sets.newHashSet("path{}", "path-2{}", "path-3{}"))),
        Arguments.of(Map.of(0, Sets.newHashSet("path{}")), Map.of(1, Sets.newHashSet("path-1{}"))),
        Arguments.of(
            Map.of(0, Sets.newHashSet("path{}", "path-01{}")),
            Map.of(1, Sets.newHashSet("path-10{}", "path-11{}")),
            Map.of(2, Sets.newHashSet("path-20{}", "path-21{}"))));
  }
}
