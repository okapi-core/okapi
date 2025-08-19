#!/usr/bin/env bash
set -euo pipefail

ENDPOINT="http://localhost:9000/api/v1/metrics/q"
TENANT="demo"
METRIC="checkout_latency_ms"
SERVICE="checkout"
INSTANCE="i-123"

# Query the last 10 seconds
NOW=$(date +%s000)               # current time in ms
START=$((NOW - 100000))           # 100s ago in ms

echo "🔎 Querying metric=$METRIC instance=$INSTANCE"

cat <<EOF | curl -s -X POST "$ENDPOINT" \
  -H "Content-Type: application/json" \
  -d @- | jq -r '
    if (.times and (.times|length) > 0)
    then
      .times as $t | .values as $v |
      range(0; $t|length) | "\($t[.]) -> \($v[.])"
    else
      "no-series"
    end
  '
{
  "tenantId": "$TENANT",
  "metricName": "$METRIC",
  "tags": {
    "service": "$SERVICE",
    "instance": "$INSTANCE"
  },
  "start": $START,
  "end": $NOW,
  "resolution": "SECONDLY",
  "aggregation": "AVG"
}
EOF
