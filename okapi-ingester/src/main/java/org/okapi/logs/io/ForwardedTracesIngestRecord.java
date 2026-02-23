package org.okapi.logs.io;

import java.util.List;

public class ForwardedTracesIngestRecord {
    int shard;
    List<LogIngestRecord> records;
}
