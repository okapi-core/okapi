package org.okapi.logs.io;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class ForwardedLogIngestRecord {
    int shard;
    List<LogIngestRecord> records;
}
