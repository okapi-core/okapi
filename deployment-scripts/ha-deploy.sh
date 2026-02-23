#!/usr/bin/env bash
set -euo pipefail

NAMESPACE=${NAMESPACE:-okapi}

kubectl create namespace "$NAMESPACE" --dry-run=client -o yaml | kubectl apply -f -

helm upgrade --install okapi-ingester helm/okapi-ingester \
  --namespace "$NAMESPACE" \
  -f deployment-scripts/values-yaml/ha/okapi-ingester-values.yaml

helm upgrade --install okapi-web helm/okapi-web \
  --namespace "$NAMESPACE" \
  -f deployment-scripts/values-yaml/ha/okapi-web-values.yaml
