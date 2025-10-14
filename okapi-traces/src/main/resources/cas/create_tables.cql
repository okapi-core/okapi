CREATE TABLE IF NOT EXISTS okapi_telemetry.okapi_spans (
  tenant_id text,
  trace_id text,
  span_id text,
  parent_span_id text,
  name text,
  start_time_ms bigint,
  end_time_ms bigint,
  duration_ms bigint,
  kind text,
  status_code text,
  status_message text,
  attributes_blob blob,
  events_blob blob,
  PRIMARY KEY ((tenant_id, trace_id), span_id)
);
--
CREATE TABLE IF NOT EXISTS okapi_telemetry.okapi_spans_by_time (
  tenant_id text,
  bucket_second bigint,
  start_time_ms bigint,
  trace_id text,
  span_id text,
  status_code text,
  duration_ms bigint,
  PRIMARY KEY ((tenant_id, bucket_second), status_code, start_time_ms, trace_id, span_id)
) WITH CLUSTERING ORDER BY (status_code ASC, start_time_ms ASC, trace_id ASC, span_id ASC);
--
CREATE TABLE IF NOT EXISTS okapi_telemetry.okapi_spans_by_duration (
  tenant_id text,
  bucket_second bigint,
  duration_ms bigint,
  trace_id text,
  span_id text,
  start_time_ms bigint,
  PRIMARY KEY ((tenant_id, bucket_second), duration_ms, trace_id, span_id)
) WITH CLUSTERING ORDER BY (duration_ms DESC, trace_id ASC, span_id ASC);
--
CREATE TABLE IF NOT EXISTS okapi_telemetry.okapi_traces_by_time (
  tenant_id text,
  bucket_second bigint,
  trace_id text,
  first_start_time_ms bigint,
  last_end_time_ms bigint,
  has_error boolean,
  PRIMARY KEY ((tenant_id, bucket_second), trace_id)
);
--
CREATE TABLE IF NOT EXISTS okapi_telemetry.okapi_spans_by_id (
  tenant_id text,
  span_id text,
  trace_id text,
  PRIMARY KEY ((tenant_id), span_id)
);
--
CREATE TABLE IF NOT EXISTS okapi_telemetry.okapi_span_count_by_minute (
  tenant_id text,
  minute_bucket bigint,
  ok_count counter,
  error_count counter,
  PRIMARY KEY ((tenant_id), minute_bucket)
);