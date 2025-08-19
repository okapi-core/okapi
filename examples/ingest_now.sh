#!/usr/bin/env bash
set -euo pipefail

ENDPOINT="${ENDPOINT:-http://localhost:9000/api/v1/metrics}"

TENANT="demo"
METRIC="checkout_latency_ms"
SERVICE="checkout"
INSTANCE="i-123"

# Portable epoch ms (works on macOS & Linux)
NOW_MS=$(( $(date +%s) * 1000 ))

curl -sS -X POST "$ENDPOINT" \
  -H "Content-Type: application/json" \
  -d @- <<JSON
{
  "tenantId": "$TENANT",
  "metricName": "$METRIC",
  "tags": { "service": "$SERVICE", "instance": "$INSTANCE" },
  "values": [120.5, 98.3, 104.2],
  "ts": [ $((NOW_MS-2000)), $((NOW_MS-1000)), $NOW_MS ]
}
JSON

echo "✅ Ingested 3 samples for $METRIC ($SERVICE/$INSTANCE)"