package org.okapi.web.ai.tools.signals;

import java.util.Collections;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;
import org.okapi.web.ai.tools.params.SpanQuery;

public class TracesSignal {
  @Getter SpanQuery spanQuery;
  List<Span> spans;

  @Builder
  public TracesSignal(SpanQuery spanQuery, @Singular List<Span> spans) {
    this.spanQuery = spanQuery;
    this.spans = spans;
  }

  public List<Span> getSpans() {
    return Collections.unmodifiableList(spans);
  }
}
