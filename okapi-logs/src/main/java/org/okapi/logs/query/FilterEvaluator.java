package org.okapi.logs.query;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.okapi.logs.io.LogPage;
import org.okapi.protos.logs.LogPayloadProto;
import org.roaringbitmap.RoaringBitmap;

public final class FilterEvaluator {
  private FilterEvaluator() {}

  public static List<LogPayloadProto> apply(LogPage page, LogFilter filter) {
    List<LogPayloadProto> out = new ArrayList<>();
    RoaringBitmap candidates = allDocs(page);
    candidates = applyFilterToCandidates(page, filter, candidates);
    for (int docId : candidates.toArray()) {
      LogPayloadProto doc = page.getLogDocs().get(docId);
      if (matchesDoc(doc, filter)) out.add(doc);
    }
    return out;
  }

  private static RoaringBitmap allDocs(LogPage page) {
    RoaringBitmap bm = new RoaringBitmap();
    if (page.getMaxDocId() >= 0) bm.add(0, page.getMaxDocId() + 1);
    return bm;
  }

  private static RoaringBitmap applyFilterToCandidates(
      LogPage page, LogFilter filter, RoaringBitmap candidates) {
    return switch (filter.kind()) {
      case LEVEL -> {
        int level = ((LevelFilter) filter).getLevelCode();
        RoaringBitmap bm = page.getLevelsInPage().get(level);
        if (bm == null) yield new RoaringBitmap();
        yield RoaringBitmap.and(candidates, bm);
      }
      case TRACE -> {
        var traceId = ((TraceFilter) filter).getTraceId();
        if (!page.getTraceIdSet().mightContain(traceId)) yield new RoaringBitmap();
        RoaringBitmap bm = new RoaringBitmap();
        List<LogPayloadProto> docs = page.getLogDocs();
        for (int i = 0; i < docs.size(); i++) {
          var d = docs.get(i);
          if (d.hasTraceId() && traceId.equals(d.getTraceId())) bm.add(i);
        }
        yield RoaringBitmap.and(candidates, bm);
      }
      case REGEX -> {
        // baseline: no trigram prefiltering
        yield candidates;
      }
      case AND -> {
        AndFilter and = (AndFilter) filter;
        RoaringBitmap left = applyFilterToCandidates(page, and.getLeft(), candidates);
        RoaringBitmap right = applyFilterToCandidates(page, and.getRight(), candidates);
        yield RoaringBitmap.and(left, right);
      }
      case OR -> {
        OrFilter or = (OrFilter) filter;
        RoaringBitmap left = applyFilterToCandidates(page, or.getLeft(), candidates);
        RoaringBitmap right = applyFilterToCandidates(page, or.getRight(), candidates);
        yield RoaringBitmap.or(left, right);
      }
    };
  }

  private static boolean matchesDoc(LogPayloadProto doc, LogFilter filter) {
    return switch (filter.kind()) {
      case LEVEL -> doc.getLevel() == ((LevelFilter) filter).getLevelCode();
      case TRACE ->
          doc.hasTraceId() && ((TraceFilter) filter).getTraceId().equals(doc.getTraceId());
      case REGEX -> {
        String regex = ((RegexFilter) filter).getRegex();
        Pattern p = Pattern.compile(regex);
        yield p.matcher(doc.getBody()).find();
      }
      case AND ->
          matchesDoc(doc, ((AndFilter) filter).getLeft())
              && matchesDoc(doc, ((AndFilter) filter).getRight());
      case OR ->
          matchesDoc(doc, ((OrFilter) filter).getLeft())
              || matchesDoc(doc, ((OrFilter) filter).getRight());
    };
  }
}
