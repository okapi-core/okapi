package org.okapi.traces.io;


import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@AllArgsConstructor
@Getter
public class ForwardedSpanRecord {
    int shard;
    List<SpanIngestionRecord> records;
}
