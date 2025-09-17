CREATE TABLE IF NOT EXISTS okapi_telemetry.gauge_sketch_secondly(
tenant_id text,
local_path text,
second_block bigint,
sketch blob,
PRIMARY KEY ((tenant_id, local_path), second_block)
) with CLUSTERING ORDER BY (second_block ASC);
--
CREATE TABLE IF NOT EXISTS okapi_telemetry.gauge_sketch_minutely(
tenant_id text,
local_path text,
minute_block bigint,
update_time bigint,
sketch blob,
PRIMARY KEY ((tenant_id, local_path), minute_block, update_time)
) with CLUSTERING ORDER BY (minute_block ASC, update_time DESC);
--
CREATE TABLE IF NOT EXISTS okapi_telemetry.gauge_sketch_hourly(
tenant_id text,
local_path text,
hr_block bigint,
update_time bigint,
sketch blob,
PRIMARY KEY ((tenant_id, local_path), hr_block, update_time)
) with CLUSTERING ORDER BY (hr_block ASC, update_time DESC);
--
CREATE TABLE IF NOT EXISTS okapi_telemetry.histo_sketch(
tenant_id text,
local_path text,
start_second bigint,
end_second bigint,
sketch blob,
PRIMARY KEY ((tenant_id, local_path), start_second)
) with CLUSTERING ORDER BY (start_second ASC);
--
CREATE TABLE IF NOT EXISTS okapi_telemetry.sums_sketch(
tenant_id text,
local_path text,
start_second bigint,
end_second bigint,
sketch blob,
PRIMARY KEY ((tenant_id, local_path), start_second)
) with CLUSTERING ORDER BY (start_second ASC);
--
CREATE TABLE IF NOT EXISTS okapi_telemetry.search_hints(
tenant_id text,
start_minute bigint,
local_path text,
metric_type text,
PRIMARY KEY ((tenant_id), start_minute)
) with CLUSTERING ORDER BY (start_minute ASC);