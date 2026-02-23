CREATE TABLE IF NOT EXISTS okapi_metrics.gauge_raw_samples (
    timestamp DateTime64(3, 'UTC'),
    resource LowCardinality(String),
    metric LowCardinality(String),
    tags Map(String, String),
    value Float32,
    minute UInt8 DEFAULT toStartOfMinute(timestamp),
    hour UInt8 DEFAULT toStartOfHour(timestamp),
    day UInt8 DEFAULT toStartOfDay(timestamp),
    month UInt8 DEFAULT toStartOfMonth(timestamp)
)
ENGINE = MergeTree
PARTITION BY (resource, toYYYYMM(timestamp))
ORDER BY (resource, metric, toUnixTimestamp(timestamp));
