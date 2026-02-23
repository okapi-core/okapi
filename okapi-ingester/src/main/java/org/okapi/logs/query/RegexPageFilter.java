package org.okapi.logs.query;

import com.google.re2j.Pattern;
import java.util.List;
import lombok.Value;
import org.okapi.abstractfilter.PageFilter;
import org.okapi.abstractio.TrigramUtil;
import org.okapi.logs.io.LogPageMetadata;
import org.okapi.primitives.BinaryLogRecordV1;

@Value
public class RegexPageFilter implements PageFilter<BinaryLogRecordV1, LogPageMetadata> {
  String regex;
  List<List<Integer>> orTrigrams;
  Pattern re2Pattern;

  public RegexPageFilter(String regex) {
    this.regex = regex;
    this.orTrigrams = TrigramUtil.getOrTrigramsBasedOnRe2(regex);
    this.re2Pattern = Pattern.compile(regex);
  }

  public static boolean allTrigramsMatch(List<Integer> trigrams, LogPageMetadata pageMeta) {
    for (var trigram : trigrams) {
      if (!pageMeta.maybeContainsTrigram(trigram)) {
        return false;
      }
    }
    return true;
  }

  @Override
  public Kind kind() {
    return Kind.REGEX;
  }

  @Override
  public boolean shouldReadPage(LogPageMetadata pageMeta) {
    boolean anyMatchFully = false;
    for (var trigramList : orTrigrams) {
      anyMatchFully = allTrigramsMatch(trigramList, pageMeta);
      if (anyMatchFully) {
        break;
      }
    }
    return anyMatchFully;
  }

  @Override
  public List<BinaryLogRecordV1> getMatchingRecords(List<BinaryLogRecordV1> record) {
    return record.stream().filter(this::matchesRecord).toList();
  }

  @Override
  public boolean matchesRecord(BinaryLogRecordV1 record) {
    return re2Pattern.matcher(record.getBody()).find();
  }
}
