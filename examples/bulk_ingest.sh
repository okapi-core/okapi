#!/bin/sh
set -eu

ENDPOINT="${ENDPOINT:-http://localhost:9000/api/v1/metrics}"
TENANT="${TENANT:-demo}"
METRIC="${METRIC:-checkout_latency_ms}"
SERVICE="${SERVICE:-checkout}"

REQS="${REQS:-100}"          # number of requests
BATCH="${BATCH:-20}"         # values per request
INSTANCES_MAX="${INSTANCES_MAX:-1000}"  # picks i-0..i-(N-1)
OUT_INSTANCES="${OUT_INSTANCES:-examples/instances.txt}"

# clean list of used instances
mkdir -p "$(dirname "$OUT_INSTANCES")"
: > "$OUT_INSTANCES"

i=1
while [ "$i" -le "$REQS" ]; do
  # portable random 0..INSTANCES_MAX-1
  R=$(od -An -N2 -tu2 /dev/urandom | tr -d ' ')
  IDX=$(( R % INSTANCES_MAX ))
  INSTANCE="i-$IDX"
  NOW_MS=$(date +%s000)

  # generate arrays
  VALUES=$(awk -v n="$BATCH" 'BEGIN{srand(); for(i=1;i<=n;i++){printf("%s%.1f", (i>1?",":""), (rand()*200))}}')
  TS=$(awk -v n="$BATCH" -v now="$NOW_MS" 'BEGIN{for(i=0;i<n;i++){printf("%s%d", (i>0?",":""), now + i*1000)}}')

  # send request
  curl -s -X POST "$ENDPOINT" \
    -H "Content-Type: application/json" \
    -d @- >/dev/null <<JSON
{
  "tenantId": "$TENANT",
  "metricName": "$METRIC",
  "tags": { "service": "$SERVICE", "instance": "$INSTANCE" },
  "values": [ $VALUES ],
  "ts": [ $TS ]
}
JSON

  # record instance (unique)
  if ! grep -qx "$INSTANCE" "$OUT_INSTANCES"; then
    echo "$INSTANCE" >> "$OUT_INSTANCES"
  fi

  i=$(( i + 1 ))
done

echo "✅ Sent $REQS requests (≈$((REQS*BATCH)) samples)"
echo "📄 Instances used: $(wc -l < "$OUT_INSTANCES") saved to $OUT_INSTANCES"