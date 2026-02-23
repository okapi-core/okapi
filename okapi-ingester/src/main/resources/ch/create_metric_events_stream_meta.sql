CREATE TABLE IF NOT EXISTS okapi_metrics.metric_events_stream_meta (
    event_type Enum('GAUGE' = 1, 'HISTO' = 2, 'SUM' = 3),
    svc LowCardinality(String),
    metric LowCardinality(String),
    tags Map(String, String),
    ts_start DateTime64(3, 'UTC'),
    ts_end DateTime64(3, 'UTC'),
    minute UInt8 DEFAULT toStartOfMinute(ts_start),
    hour UInt8 DEFAULT toStartOfHour(ts_start),
    day UInt8 DEFAULT toStartOfDay(ts_start),
    month UInt8 DEFAULT toStartOfMonth(ts_start)
)
ENGINE = MergeTree
PARTITION BY toYYYYMM(ts_start)
ORDER BY (svc, metric, event_type, toUnixTimestamp(ts_start));
