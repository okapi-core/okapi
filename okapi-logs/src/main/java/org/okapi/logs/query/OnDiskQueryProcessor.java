package org.okapi.logs.query;

import com.google.common.primitives.Ints;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.okapi.logs.config.LogsCfg;
import org.okapi.logs.index.PageIndex;
import org.okapi.logs.index.PageIndexEntry;
import org.okapi.logs.io.LocalPageReader;
import org.okapi.logs.io.LogFileWriter;
import org.okapi.logs.io.LogPageSerializer;
import org.okapi.logs.stats.StatsEmitter;
import org.okapi.protos.logs.LogPayloadProto;
import org.roaringbitmap.RoaringBitmap;
import org.springframework.stereotype.Service;

@Service
public class OnDiskQueryProcessor implements QueryProcessor {
  private final LogFileWriter writer;
  private final StatsEmitter stats;
  private final LocalPageReader pageReader;
  private final IndexCache indexCache = new IndexCache(256);
  private final LogsCfg logsCfg;

  public OnDiskQueryProcessor(LogsCfg cfg, StatsEmitter stats) {
    this.writer = new LogFileWriter(cfg);
    this.stats = stats;
    this.pageReader = new LocalPageReader(stats, 32);
    this.logsCfg = cfg;
  }

  @Override
  public List<LogPayloadProto> getLogs(
      String tenantId, String logStream, long start, long end, LogFilter filter, QueryConfig cfg)
      throws IOException {
    List<LogPayloadProto> out = new ArrayList<>();
    for (TimePartition p : existingHourPartitions(tenantId, logStream, start, end)) {
      Path idx = p.path().resolve("logfile.idx");
      Path bin = p.path().resolve("logfile.bin");
      List<PageIndexEntry> entries = loadIndex(idx);
      if (entries.isEmpty()) continue;
      stats.indexEntriesLoaded(entries.size());
      for (PageIndexEntry e : entries) {
        if (e.getTsEnd() < start || e.getTsStart() > end) continue;
        stats.pagesScanned(1);
        out.addAll(applyFilterFromDisk(bin, e.getOffset(), filter));
      }
    }
    return out;
  }

  private record TimePartition(long blockStart, Path path) {}

  private List<TimePartition> existingHourPartitions(
      String tenantId, String logStream, long start, long end) throws IOException {
    Path base = writer.partitionDir(tenantId, logStream);
    if (!Files.exists(base)) return List.of();
    List<TimePartition> hours = new ArrayList<>();
    var hrStart = start / logsCfg.getIdxExpiryDuration();
    var hrEnd = end / logsCfg.getIdxExpiryDuration();
    try (var dir = Files.newDirectoryStream(base)) {
      for (Path p : dir) {
        String name = p.getFileName().toString();
        var hr = Integer.parseInt(name);
        if (hr >= hrStart && hr <= hrEnd) hours.add(new TimePartition(hr, p));
      }
    }
    Collections.sort(hours, Comparator.comparingLong(TimePartition::blockStart));
    return hours;
  }

  private List<PageIndexEntry> loadIndex(Path idx) throws IOException {
    if (!Files.exists(idx)) return List.of();
    long size = Files.size(idx);
    long mtime = Files.getLastModifiedTime(idx).toMillis();
    List<PageIndexEntry> cached = indexCache.get(idx, size, mtime);
    if (cached != null) {
      stats.cacheHit("index");
      return cached;
    }
    stats.cacheMiss("index");
    List<PageIndexEntry> entries = new PageIndex(idx).readAll();
    indexCache.put(idx, size, mtime, entries);
    return entries;
  }

  private List<LogPayloadProto> applyFilterFromDisk(Path bin, long pageOffset, LogFilter filter)
      throws IOException {
    if (filter instanceof LevelFilter lf) {
      return applyLevelFilter(bin, pageOffset, lf.getLevelCode());
    } else if (filter instanceof TraceFilter tf) {
      return applyTraceFilter(bin, pageOffset, tf.getTraceId());
    } else if (filter instanceof RegexFilter rf) {
      return applyRegexFilter(bin, pageOffset, rf.getRegex());
    }
    // Fallback to deserialize for complex filters
    byte[] page = readFullPage(bin, pageOffset);
    var logPage = LogPageSerializer.deserialize(page);
    return FilterEvaluator.apply(logPage, filter);
  }

  private byte[] readFullPage(Path bin, long pageOffset) throws IOException {
    LocalPageReader.Header h = pageReader.readHeader(bin, pageOffset);
    int total = 80 + h.lenTri() + h.lenLvl() + h.lenBloom() + h.lenDocs() + 4;
    return pageReader.readRange(bin, pageOffset, total);
  }

  private List<LogPayloadProto> applyLevelFilter(Path bin, long pageOffset, int level)
      throws IOException {
    LocalPageReader.Header h = pageReader.readHeader(bin, pageOffset);
    RoaringBitmap bm = pageReader.readLevelBitmap(bin, pageOffset, h, level);
    if (bm == null || bm.isEmpty()) return List.of();
    int[] docIds = bm.toArray();
    Arrays.sort(docIds);
    return pageReader.readDocsByIds(bin, pageOffset, h, docIds);
  }

  private List<LogPayloadProto> applyTraceFilter(Path bin, long pageOffset, String traceId)
      throws IOException {
    LocalPageReader.Header h = pageReader.readHeader(bin, pageOffset);
    if (!pageReader.bloomMightContain(bin, pageOffset, h, traceId)) return List.of();
    // No docId index for traceId; scan all docs in the page
    byte[] sizes = pageReader.readDocsSizes(bin, pageOffset, h);
    int pos = 0;
    int nDocs = Ints.fromByteArray(slice(sizes, pos, 4));
    pos += 4;
    List<Integer> ids = new ArrayList<>();
    for (int i = 0; i < nDocs; i++) {
      ids.add(i);
      pos += 4;
    }
    int[] docIds = ids.stream().mapToInt(Integer::intValue).toArray();
    List<LogPayloadProto> docs = pageReader.readDocsByIds(bin, pageOffset, h, docIds);
    List<LogPayloadProto> out = new ArrayList<>();
    for (var d : docs) if (d.hasTraceId() && traceId.equals(d.getTraceId())) out.add(d);
    return out;
  }

  private List<LogPayloadProto> applyRegexFilter(Path bin, long pageOffset, String regex)
      throws IOException {
    LocalPageReader.Header h = pageReader.readHeader(bin, pageOffset);
    Pattern p = Pattern.compile(regex);
    // Simple heuristic: if regex has only letters/digits/space/._- use trigram prefilter
    boolean simple = regex.matches("[A-Za-z0-9 ._\\-]+");
    int[] docIds;
    if (simple) {
      int[] tris = asciiTrigrams(regex);
      RoaringBitmap bm = pageReader.readTrigramIntersection(bin, pageOffset, h, tris);
      if (bm == null || bm.isEmpty()) return List.of();
      docIds = bm.toArray();
    } else {
      byte[] sizes = pageReader.readDocsSizes(bin, pageOffset, h);
      int n = Ints.fromByteArray(slice(sizes, 0, 4));
      docIds = new int[n];
      for (int i = 0; i < n; i++) docIds[i] = i;
    }
    Arrays.sort(docIds);
    List<LogPayloadProto> docs = pageReader.readDocsByIds(bin, pageOffset, h, docIds);
    List<LogPayloadProto> out = new ArrayList<>();
    for (var d : docs) if (p.matcher(d.getBody()).find()) out.add(d);
    return out;
  }

  private static byte[] slice(byte[] a, int off, int len) {
    byte[] r = new byte[len];
    System.arraycopy(a, off, r, 0, len);
    return r;
  }

  private static int[] asciiTrigrams(String s) {
    if (s == null || s.length() < 3) return new int[0];
    int n = s.length();
    int count = Math.max(0, n - 2);
    int[] out = new int[count];
    int j = 0;
    for (int i = 0; i + 2 < n; i++) {
      char c0 = s.charAt(i);
      char c1 = s.charAt(i + 1);
      char c2 = s.charAt(i + 2);
      if (c0 < 128 && c1 < 128 && c2 < 128) {
        int idx = (c0 & 0x7F) | ((c1 & 0x7F) << 7) | ((c2 & 0x7F) << 14);
        out[j++] = idx;
      }
    }
    if (j == out.length) return out;
    return Arrays.copyOf(out, j);
  }

  private static final class IndexCache {
    private final int capacity;
    private final LinkedHashMap<Key, List<PageIndexEntry>> map;

    IndexCache(int capacity) {
      this.capacity = capacity;
      this.map =
          new LinkedHashMap<>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<Key, List<PageIndexEntry>> eldest) {
              return size() > IndexCache.this.capacity;
            }
          };
    }

    synchronized List<PageIndexEntry> get(Path idx, long size, long mtime) {
      return map.get(new Key(idx, size, mtime));
    }

    synchronized void put(Path idx, long size, long mtime, List<PageIndexEntry> entries) {
      map.put(new Key(idx, size, mtime), entries);
    }

    private record Key(Path path, long size, long mtime) {}
  }
}
