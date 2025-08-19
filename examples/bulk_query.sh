#!/bin/sh
set -eu

ENDPOINT="${ENDPOINT:-http://localhost:9000/api/v1/metrics}"
TENANT="${TENANT:-demo}"
METRIC="${METRIC:-checkout_latency_ms}"
SERVICE="${SERVICE:-checkout}"
INSTANCES_FILE="${INSTANCES_FILE:-examples/instances.txt}"

NOW=$(date +%s000)
# widen window to cover recent ingest (last 5 minutes)
START=$(( NOW - 5*60*1000 ))

# instance list priority: file -> env INSTANCES -> default small set
if [ -f "$INSTANCES_FILE" ]; then
  INSTANCES=$(cat "$INSTANCES_FILE")
elif [ -n "${INSTANCES:-}" ]; then
  INSTANCES=$INSTANCES
else
  INSTANCES="i-1
i-2
i-3
i-4
i-5"
fi

echo "$INSTANCES" | while IFS= read -r INSTANCE; do
  [ -z "$INSTANCE" ] && continue
  echo "🔎 Querying instance=$INSTANCE"
  cat <<EOF | curl -sS -X POST "$ENDPOINT/q" \
    -H "Content-Type: application/json" \
    -d @- | jq -r '
      if (.times and .values and (.times|length) > 0 and (.values|length) > 0)
      then "\(.times) -> \(.values)"
      else "no-series"
      end
    '
{
  "tenantId": "$TENANT",
  "metricName": "$METRIC",
  "tags": { "service": "$SERVICE", "instance": "$INSTANCE" },
  "start": $START,
  "end": $NOW,
  "resolution": "SECONDLY",
  "aggregation": "AVG"
}
EOF
done
