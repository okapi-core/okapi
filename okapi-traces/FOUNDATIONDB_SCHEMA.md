# FoundationDB Schema for okapi-traces

## Key Structure

Each span is stored with a key of the form:

    trace:{traceId}:{spanId}

- **trace**: A fixed prefix used as a namespace.
- **traceId**: Identifies the trace to which the span belongs.
- **spanId**: Uniquely identifies a span within the trace.

## Design Rationale

- **Write Optimization**: The key structure is optimized for low-latency writes. By using a composite key that starts with a static prefix followed by trace-specific identifiers, we minimize write contention.
- **Efficient Reads**: Retrieving all spans for a given trace is efficient. A range scan can be performed on keys with the prefix `Tuple.from("trace", traceId)` to collect all associated spans.
- **Scalability**: FoundationDB's ordered key-value storage is well-suited for range queries and high throughput of random writes.

## Future Considerations

- Additional indexes or key transformations may be applied if query patterns change.
- Consider binary encoding or compression of span data for further optimization.
