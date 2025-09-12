package org.okapi.traces.storage;

import com.apple.foundationdb.Database;
import com.apple.foundationdb.FDB;
import com.apple.foundationdb.tuple.Tuple;
import org.okapi.traces.model.Span;
import org.springframework.stereotype.Repository;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

@Repository
public class TraceRepository {

    private Database db;

    @PostConstruct
    public void init() {
        // Initialize FoundationDB with the selected API version
        FDB fdb = FDB.selectAPIVersion(620);
        db = fdb.open();
    }

    public List<Span> getSpansByTraceId(String traceId) {
        // FoundationDB key schema: "trace:{traceId}:{spanId}"
        byte[] prefix = Tuple.from("trace", traceId).pack();
        return db.read(tr -> {
            List<Span> spans = new ArrayList<>();
            // TODO: implement range query over the keys with the given prefix
            // and deserialize the retrieved values into Span objects.
            return spans;
        });
    }

    // Future methods for writing spans can be added here.
}
