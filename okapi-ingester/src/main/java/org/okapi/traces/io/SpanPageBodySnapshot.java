package org.okapi.traces.io;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.okapi.primitives.BinarySpanRecordV2;

@AllArgsConstructor
@Getter
public class SpanPageBodySnapshot {
  List<BinarySpanRecordV2> spans;
}
