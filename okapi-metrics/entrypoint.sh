#!/usr/bin/env sh
set -eu

# Compatibility for common typo
if [ "${JAVA_OPTS:-}" = "" ] && [ "${JAVA_OPS:-}" != "" ]; then
  JAVA_OPTS="$JAVA_OPS"
fi

# Run; everything after the image name (e.g. --server.port=9000) goes to Spring
exec java ${JAVA_OPTS:-} -jar /app/okapi-metrics.jar "$@"