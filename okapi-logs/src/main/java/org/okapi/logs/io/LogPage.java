package org.okapi.logs.io;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.Singular;
import lombok.ToString;
import org.okapi.protos.logs.LogPayloadProto;
import org.roaringbitmap.RoaringBitmap;

@Getter
@ToString
public class LogPage {
  @Setter(AccessLevel.PACKAGE)
  private long tsStart = Long.MAX_VALUE;

  @Setter(AccessLevel.PACKAGE)
  private long tsEnd = Long.MIN_VALUE;

  @Setter(AccessLevel.PACKAGE)
  private int maxDocId = -1;

  private final Map<Integer, RoaringBitmap> trigramMap;
  private final Map<Integer, RoaringBitmap> levelsInPage;
  private final BloomFilter<CharSequence> traceIdSet;
  private final List<LogPayloadProto> logDocs;

  @Builder
  public LogPage(
      @Singular("trigramEntry") Map<Integer, RoaringBitmap> trigramMap,
      @Singular("levelEntry") Map<Integer, RoaringBitmap> levelsInPage,
      BloomFilter<CharSequence> traceIdSet,
      @Singular("doc") List<LogPayloadProto> logDocs,
      Integer expectedInsertions) {
    // Ensure mutable collections even if Lombok @Builder provided unmodifiable ones
    this.trigramMap =
        (trigramMap != null) ? new HashMap<>(trigramMap) : new HashMap<>();
    this.levelsInPage =
        (levelsInPage != null) ? new HashMap<>(levelsInPage) : new HashMap<>();
    this.traceIdSet =
        (traceIdSet != null)
            ? traceIdSet
            : BloomFilter.create(
                Funnels.stringFunnel(StandardCharsets.UTF_8),
                expectedInsertions != null ? expectedInsertions : 20000);
    this.logDocs = (logDocs != null) ? new ArrayList<>(logDocs) : new ArrayList<>();
  }

  public int sizeInDocs() {
    return logDocs.size();
  }

  public void append(long tsMillis, String traceId, int level, String body) {
    int docId = ++this.maxDocId;
    this.tsStart = Math.min(this.tsStart, tsMillis);
    this.tsEnd = Math.max(this.tsEnd, tsMillis);

    LogPayloadProto.Builder b =
        LogPayloadProto.newBuilder().setTsMillis(tsMillis).setLevel(level).setBody(body);
    if (traceId != null && !traceId.isEmpty()) {
      b.setTraceId(traceId);
      this.traceIdSet.put(traceId);
    }
    this.logDocs.add(b.build());

    // levels -> docIds
    RoaringBitmap levelBm = this.levelsInPage.computeIfAbsent(level, k -> new RoaringBitmap());
    levelBm.add(docId);

    // trigrams
    for (int tri : TrigramUtil.extractAsciiTrigramIndices(body)) {
      RoaringBitmap bm = this.trigramMap.computeIfAbsent(tri, k -> new RoaringBitmap());
      bm.add(docId);
    }
  }
}
