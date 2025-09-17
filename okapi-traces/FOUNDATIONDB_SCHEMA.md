# FoundationDB Schema for okapi-traces

This module stores OpenTelemetry spans with a schema optimized for low-latency writes and fast lookups for the supported queries.

All keys are encoded with `Tuple` (FoundationDB tuple layer). Strings noted below are tuple elements, not concatenated strings.

## Primary Records

Span record (for listing spans within a trace):

    ("tr", tenantId, appId, traceId, spanId) -> JSON(span)

- Value contains the span metadata (name, parentSpanId, start/end times, attributes, status, etc.).
- This allows efficient range scans for all spans in a trace via the prefix `("tr", tenantId, appId, traceId)`.

## Secondary Indexes

1) Span ID lookup (span metadata by span-id):

    ("sid", tenantId, appId, spanId) -> Tuple(traceId)

- Used to fetch the primary span record when only `spanId` is provided.

2) Duration index (list spans by duration for an app and time window):

    ("dur", tenantId, appId, bucketHrEpochMs, shard, negDurationMs, traceId, spanId) -> NIL

- `bucketHrEpochMs` is the span start timestamp truncated to the hour (epoch milliseconds).
- `shard` is a small integer hash of spanId (`[0, DURATION_SHARDS)`) to spread writes and avoid hot keys.
- `negDurationMs` is the negative duration in milliseconds so that lexicographic sort yields longest spans first.
- Efficient scan is done by iterating hour buckets over the required window and merging results in memory.

## Notes on Write Optimization

- All writes for an ingest batch are performed in a single transaction (one set per record/index entry).
- Duration index includes a small hash shard to distribute keys within a hot time bucket.
- Tuple-encoded keys avoid string processing overhead and keep ordering properties useful for range scans.

## Multi‑tenancy

- All keys include `tenantId` and `appId` as leading discriminators after the namespace prefix.
- These are supplied by request headers `X-Okapi-Tenant` and `X-Okapi-App`.

## Future Considerations

- Add pagination for duration queries if needed for very large result sets.
- Consider storing values in a compact binary format if value size becomes a bottleneck; JSON is used for simplicity now.
